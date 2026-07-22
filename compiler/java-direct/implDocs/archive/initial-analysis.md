
# Initial Analysis

Let's revisit the plans of implementing the `java-direct` module in Kotlin codebase.
Your goal is to create a text describing the implementation approach with all visible details, to continue the implementation.

I will start describing the goals, the current state, analogous components, currently obvious difficulties, possible solutions and 
other aspects. After I finish describing the initial details, I will tell you to start interviewing me on the missing pieces needed 
to create the comprehensive description. Before and during the interview, use not only directly mentioned references, but also other sources
and documents in the current project, mostly the ones located in the `compiler/` directory, to understand the current structure and the 
possible approaches to the implementation.

After finishing the interview, either then you decide you have no more questions, or I will command you to stop and generate the report,
generate the output document (in the chat or in a file in the `compiler/java-direct/` directory) containing the description.

The high-level goal, overarching this work and some more tasks, is to get rid of IntelliJ platform dependency in the compiler, which
causes quite some problems, because it wasn't designed for the compiler usecase.

The goal of this particular work is to replace "Java parsing and resolution" functionality, that we are currently using from the IntelliJ platform,
with a custom implementation.

The Kotlin language, in contrast to most (or all) other JVM languages, supports bidirectional interoperability with Java, which means
that Java sources may coexist with Kotlin ones in a Kotlin project, and references may go both ways and transitively. So one may have a
class defined in a Kotlin file, and a Java source that uses it, and then another Kotlin file that uses the class from the Java source, etc.
In practice it means that when the Kotlin compiler compiles a Kotlin file, there could be no binary class representation of a Java source
it references. So the compiler needs to look at the Java source file and extract visible declarations from it. Basically, it means that
the Kotlin compiler should contain "a half" of a Java compiler in it. (Strictly speaking - not a half, but it beside the point.)

The current implementation of this subsystem is based on IntelliJ platform, which can parse and analyse java sources, providing PSI files, 
containing all information Kotlin compiler needs. On top of these PSI interfaces, the compiler has an abstraction layer named "Java Model", 
which in a row is used inside FIR part to access and process relevant Java defintions.
In addition to it, the Kotlin compiler parts that provide external symbols for the compiler, for declarations from binary dependencies, use
some shared infrastructure for binary classes deserialization, so if Java subsystem from the platform references some external declaration 
from dependency, and then some Kotlin declaration references the same declaration, the declaration deserialized only once and shared between 
the both parts.
The main "Java Model" interfaces are defined in `core/compiler.common.jvm/src/org/jetbrains/kotlin/load/java/structure/javaElements.kt`.
The platform-based implementation is defined in the `compiler/frontend.common.jvm` module.

There is an alternative and somewhat outdated implementation based on the unofficial `javac` APIs. It is located in `compiler/javac-wrapper`
module. Besides "wrong" API use, it is hardly tested and not really kept up to date, but could be used as a reference.

There are also reflection specific and binary class Java model implementation, but they have different usage scenarios.

So we need to implement a new model that will not use the IntelliJ platform, should be able to read Java sources, parse them, build Java 
model out of them, and then be able to "resolve" the referenced entities in this model. 

Attached is the initial discussion document, with the principal decisions taken.

The initial prototype of the new implementation is located in the `compiler/java-direct` project.
It uses the KMP java parser library for the parsin tasks. Note that although this library is a part of the IntekkiJ project, it is a
separate library and does not "pull" the IntelliJ platform with it. So its use in the module is acceptable and good.

The parsing part is relatively easy, and if there are things to do on top of the current prototype, they should be straightforward.

The buidling of the Java model is a bit more involved, although not by much. One should filter out all the unnecessary parts from the AST
and carefully build the model.

The most difficult part is to build a resolution mechanism. If we see a name referenced in the Java source, we should first understand, in 
which scopes we should search the name. In a simplified form, what prefixes (FQNs) we should try to add to the name for the lookups.
These are defined by the current package, imports, and the location within the class definition (e.g. one may reference a name from the 
enclosing class).
The fact that we don't need to resolve references inside method bodies, and that Java resolve is much simpler than the Kotlin one, makes
the resolution part somewhat easier. The current `javac-wrapper` implementation is likely a good first approximation.

And then the lookup process itself should be forwarded to the FIR infrastructure (session -> symbol provider), so that the symbols remain
to be shared between the Kotlin and Java subsystems. Or maybe the some shortcuts should be made for the declarations handled by the Java 
model itself - i.e. declarations from the current or other java sources handled by the Java model. Or maybe not, if these declarations
are properly registered in the session on some initial phase.

One aspect that should not be forgotten, that the Java model is supposed to be lazy; it supposed to pull the external declarations only
when they are actually needed. And may be even the declarations directly defined in the sources, although this may interfere with
other goals. The scenario that we should keep in mind is that in some module we may have only a few kotlin source file among many java ones
(initial stages of Kotlin migration or some other scenario then the Kotlin plays a secondary role). And this Kotlin file may reference
none or very few Java declarations. In this scenario we should probably not analyze the Java sources, unless they are actually referenced
from the Kotlin file. But the Kotlin resolution is quite complicated, so at some point we may still need to know at least "top-level names"
of the Java declarations.

And from the Kotlin project infrastructure point of view, the new java model implementation should be tested against all (or most) test
cases in the project that involve Java. That requires an implementation of a pluggable "Java facade" for the test infrastructure, as well as
for the CLI compiler. The work is partially started in the `java-direct` module.

It should be noted that the "Analysis API" part of the Kotlin project, should not be affected by this work, at least functionally. It
supposed to be used inside the IDE enbvironment, where the old Java model implementation based on the IntelliJ platform is perfectly 
available and fine to use.

Now analyze the given references and relevant project part, and start interviewing me on the missing pieces.

## Interview Questions

1. Resolution Architecture & FIR Integration
   Looking at the code, I see that FirJavaFacade converts Java classes to FIR declarations. For the new java-direct implementation:
   Q1.1: Should we resolve Java type references (in supertypes, field types, method return types, etc.) by directly querying the FIR session's symbol providers, or should we build a separate resolution layer within the Java model first?
   Q1.2: The javac-wrapper uses ClassifierResolver and IdentifierResolver which implement scope-based resolution (imports, current class, enclosing classes, etc.). Should the new implementation follow a similar architecture, or should we delegate resolution earlier to FIR?
   Q1.3: How should we handle the circular dependency issue: FIR needs the Java model to build FIR declarations, but resolution within the Java model might need access to FIR symbol providers. What's the proper layering?
2. Laziness & Performance
   You mentioned laziness is important:
   Q2.1: What exactly should be lazy? Should we:
   Parse files only when their classes are requested? (Current prototype indexes eagerly)
   Build Java model elements (supertypes, members) lazily on first access?
   Resolve type references lazily?
   Q2.2: The current JavaClassFinderOverAstImpl builds an index eagerly in buildIndex(). Is this acceptable, or should discovery be lazy too?
   Q2.3: For the scenario with "few Kotlin files among many Java files" - how do we balance between not parsing unnecessary Java files and having enough information for name resolution? Does FIR need to know all available class names upfront?
3. Constant Evaluation & Annotation Processing
   The javac-wrapper has ConstantEvaluator for annotation arguments:
   Q3.1: Should we implement constant evaluation within the java-direct module, or can we reuse existing Kotlin constant evaluation infrastructure?
   Q3.2: Annotation arguments can reference other classes (e.g., @Target(ElementType.FIELD)). How should we handle resolution of such references within annotations?
   Q3.3: Are there specific Java features in annotation processing that need special attention (enum constants, class literals, array values)?
4. Type Parameter & Generic Type Handling
   Q4.1: The current prototype has basic JavaTypeParameterOverAst. How should we handle:
   Type parameter bounds that reference other type parameters?
   Type parameter bounds that reference the containing class itself (recursive bounds)?
   Wildcards in generic types?
   Q4.2: Should type parameter resolution be handled within the Java model or delegated to FIR type system?
5. Light Classes & Kotlin-to-Java References
   Q5.1: You mentioned that Java sources can reference Kotlin classes. In the new implementation, how should this work? Should JavaClassFinderOverAstImpl be aware of Kotlin declarations, or is this handled at a higher level?
   Q5.2: The FirJavaElementFinder creates PSI stubs for Kotlin classes. Do we need an equivalent mechanism in java-direct, or is this problem avoided entirely?
6. Testing & Integration
   Q6.1: You mentioned "pluggable Java facade for test infrastructure". Looking at VfsBasedProjectEnvironmentOverAst, is this the right integration point? Are there other places where we need to plug in the new implementation?
   Q6.2: What's the migration strategy? Should we:
   Make it opt-in via a flag?
   Test in parallel with existing implementation?
   Have a gradual rollout plan?
   Q6.3: Are there specific test suites or test categories that are most important to validate first?
7. Scope & Limitations
   Q7.1: Java records, sealed classes, and other modern Java features - what's the priority? Should the initial implementation support all Java features that the PSI-based implementation supports?
   Q7.2: Inner classes, local classes, anonymous classes - the prototype handles named inner classes. What about other class kinds?
   Q7.3: Are there any Java features we can explicitly NOT support in the initial version?
8. Binary Dependencies & Classpath
   Q8.1: When a Java source references a class from a binary dependency (jar), how should this work? Should JavaClassFinderOverAstImpl delegate to another finder for non-source classes?
   Q8.2: The javac-wrapper has complex logic for sharing symbols between Java and Kotlin parts (symbolBasedClassesCache). How should we ensure symbols from binary dependencies are shared correctly?
9. Error Handling & Resilience
   Q9.1: How should we handle malformed Java source files? Should parsing errors be fatal, or should we provide partial Java model elements?
   Q9.2: If type resolution fails (e.g., reference to non-existent class), what should the Java model return? Error types? Nulls?
10. Implementation Priorities
    Q10.1: What's the most critical missing piece right now? Is it:
    Resolution mechanism?
    Constant evaluation?
    Test infrastructure integration?
    Something else?
    Q10.2: Are there any hard deadlines or milestones for this work?
    Please answer the questions that are most important for creating a comprehensive implementation plan. Feel free to skip questions that are obvious from context or defer decisions that can be made later during implementation.

## Answers

1. First of all, the `FirJavaFacade` implementations should not be generally altered, but we should instead implement a `JavaClassFinder` that 
supplies the data to the current implementation.

A1.1: There is not an easy answer. But we probably should start with a layer that keeps information about the declarations "local" for the 
current Java source file, and delegate the rest to the FIR symbol providers.

A1.2: Similarly to the previous answer, let's start from a scope that is aware of the "local" declarations, and delegate the rest to the FIR 
symbol providers.

A1.3: This should be handled by the "laziness" of the Java model. In particular:
- we might need to create an index of the top-level names of the java declarations for the given set of source files belonging to the project. The initial implementation can be as easy as a file system structure mapping, based on the assumption that the convention (dirs path from the source root == package, filename == class name). This is a generally incorrect assumption, but it should be enough to start.
- so we don't initiate any parsing unill we got a request for a symbol from Kotlin resolution (since the Java model is exposed via a symbol provider to Kotlin)
- if the requested symbol is in our index, and not yet parsed, we open file, parse it and create the model.
- at this moment, Kotlin top-level declatrations that could be referenced from Java sources are already registered and available via FIR symbol providers.

A2.1: Partially answered in the A1.3. On top of that
- after parsing we do not resolve any references that we encountered, just store them as abstract name references
- as soon as the abstract reference get accessed (presumably by the FIR Kotlin resolution), we resolve the reference using the FIR symbol providers, and return the resolved declaration (storing the resolved reference instead of the abstract one in the model for caching purposes)

A2.2: I suspect that we need to build top-level classes index eagerly, otherwise the scheme I described above will not work.

A2.3: Partially answered in A1.3. We most likely will need to avoid file/dir naming assumptions at the end, so maybe we'll need a kind of pre-parsing to inhabit the index first.

3. Yes, this is a very good point about the constant evaluator and annotation processing. In general we probably should not implement the evaluator, as it is done in the `javac-wrapper`, but rather try to reuse `FirConstantEvaluationProcessor` and related entities. But it may be not too easy. I have solved a similar problem for the scripting compilation with LightTree recently, so there is a chance to reuse the same approach. In any case, we can have initial implementation that unly supports primitive types as annotation arguments, and no constant evaluation, and come back to it later.  

A3.1: Answered above
A3.2: Answered above
A3.3: Yes. This may make the answer above incomplete. We should mark it as a potential problem to come back later.

4. I don't have answers to the generic type handling questions yet. It would be nice to delegat the resolution to FIR, if possible, so this should be the first step. But then we should carefully analyze, what use cases we may miss.

5. Light classes (and PSI stubs) should not be present in this implementation of the Java model, because we are providing the Kotlin symbols for the Java model directly from the FIR symbol providers. Basically the Java model should be able to use FIR symbols directly internally. This is one of the main value propositions of the new implementation, that we won't need to support very fragile Light CLasses.

6. Testing

A6.1: I think this is the riht integration point, at least while we haven't found any obstacles. Another place could be the test ifrastructure itself.
A6.2: We can try a gradual approach:
- first make dedicated unit tests for the simple cases, without integrating into test infra and CLI pipeline
- integrate into the test infrastructure. It may or may not require integration via the CLI infrastructure via e.g. `VfsBasedProjectEnvironmentOverAst`.
- integrate into cli pipeline via a flag.
A6.3: I'm not sure I can name some specifi ones. In general probably the ones that I already started to implement in the module, are worth to finish first.

7. We need to support all java features, that are exposed to Kotlin, at the end. But we can start say from Java 8 or 9 and gradually add features from the moder releases.

8. Dependencies

A8.1: The dependencies lookup should be implemented via FIR infrastructure (FIR session -> symbol provider).
A8.2: If we implement proper delegation to FIR symbol providers, the separate logic for sharing symbols is hopefully not needed anymore.

9. Error handling

A9.1: Parsing errors should be fatal
A9.2: Resolution errors should be reported as diagnostics via FIR mechanisms.

10. Priorities

A10.1: The most important thing at the moment is the test infrastructure integration, because it will allow us to validate the subsequent implementation steps gradually. The second is the resolution, because it is likely most difficulties and reveal most problems with the implementation. The sooner we'll discover the problem the better.

## Final Clarification Questions

11. Index & Discovery Strategy
    Based on A1.3 and A2.2-A2.3:
    Q11.1: You mentioned we might need "pre-parsing to inhabit the index" to avoid filename assumptions. Should the initial implementation:
    Start with simple filesystem-based indexing (package from path, class name from filename)?
    Then iterate to add actual pre-parsing for correctness?
    Q11.2: When you say "top-level classes index", do you mean:
    Only top-level class names per package (not nested classes)?
    This index is used by JavaSymbolProvider.symbolNamesProvider.getTopLevelClassifierNamesInPackage()?
12. Resolution Layer Architecture
    Based on A1.1, A1.2, A2.1:
    Q12.1: For the "local scope" that's aware of local declarations, should we:
    Create a JavaTypeResolver class similar to javac-wrapper's ClassifierResolver?
    Store unresolved references as strings/names in the Java model initially?
    Resolve them lazily via FIR symbol providers when accessed?
    Q12.2: When you say "delegate to FIR symbol providers", you mean:
    Call FirSession.symbolProvider.getClassLikeSymbolByClassId() to look up external types?
    Then convert the FirRegularClassSymbol back to a JavaClass representation (or keep it as symbol)?
13. Java Model Lifecycle
    Q13.1: Looking at FirJavaFacade.convertJavaClassToFir(), it creates FirJavaClass from JavaClass. In our new implementation:
    Should JavaClassOverAst instances be cached long-term, or created on-demand each time?
    Should we maintain the same instance for the same class, or can we recreate it?
    Q13.2: For lazy resolution of type references (A2.1), where exactly should resolution happen?
    In the getter of JavaClassOverAst.supertypes?
    Or at the FIR conversion layer when FirJavaFacade builds FirJavaClass?
14. Test Infrastructure - Immediate Next Steps
    Since you said "test infrastructure" is the most important thing (A10.1):
    Q14.1: What's the concrete next step for test infrastructure?
    Implement JavaClassFinderOverAstFactory.createJavaClassFinder() properly?
    Make VfsBasedProjectEnvironmentOverAst work correctly with test framework?
    Wire everything through JavaDirectComponentRegistrar?
    Q14.2: Should the plugin be enabled via a compiler argument flag, or should tests explicitly use VfsBasedProjectEnvironmentOverAst?
15. Scope of Initial Implementation
    Q15.1: For the absolute minimum viable implementation to get first tests passing, can we:
    Skip annotation argument constant evaluation entirely (treat as unsupported)?
    Support only simple cases: classes, methods, fields, basic types?
    Defer generics/type parameters resolution to FIR even if imperfect initially?
    Q15.2: What would be the simplest test case to target first? Something like:``` java
    // test/Simple.java
    package test;
    public class Simple {
    public String field;
    }
```
``` kotlin

  // test.kt
  fun test() = test.Simple().field
```


After you answer these questions, I'll generate the comprehensive implementation document. Or if you feel I have enough information already and want to skip some questions, let me know and I'll proceed with the document generation.

## answers:

11. Index & Discovery Strategy:

A11.1: Actually I'm now thinking that simplified strategy will not work for the test infrastructure. So, let’s do the following instead:
- preparsing - read files and search for the class definitions, defined by a set of regexes. And extract names from it. The regexes should catch all possible ways of defining the top level class in java source, but it may also output some false positives. This is sufficient to organize the initial index.
- we also need to extract the `package` directive from the file.
- the index should group declaration by file, so when the request coms for a particolar name, so we need to actually parse the file, we'll be able to replace all names in the index that belong to this file to the precisely extracted ones (via parsing).

A11.2: Nested classes will not be requested by the resolution without requesting the containing class first. So we can ignore nested classes on the preparsing, if we can distinguish them from top level ones. One simplification strategy could be to take only the first class declaration from each file.

12. Resolution Layer Architecture:

A12.1: No, I actually meant that after parsing we can walk the AST and extract all defined names into this "local scope", This could be a cache: name -> java model entit, while the latter could be lazily constructed from the AST, or eagerly after parsing. (We may decide about this later). The idea is that the names we get from parsing the current file need not to go via an external resolution mechanism, and can be handled locally.

A12.2: Yes to the first part, call FirSession.symbolProvider.getClassLikeSymbolByClassId() to look up external symbols. But keep it as a symbol or wrap it into a JavaModel entity, if needed by the Java model API.

13. Java Model Lifecycle:

Q13.1: They should be cached long-term (likely have the same lifetime as the `JavaClassFinder` implementation.)

Q13.2: At the Java Model level, e.g. `JavaClassOverAst.supertypes` or in other getters. `FirJavaFacade` should not be modified, and should rely on the Java Model interfaces.

14. Test Infrastructure

Q14.1: Yes to all subquestions

Q14.2: This could be the simplest test case to target first.

## generating document:


compiler/java-direct/IMPLEMENTATION_PLAN.md
Document Summary
The 12-section plan covers:
Background - Motivation, current architecture, and decision rationale
Architecture - Component stack and key principles
Detailed Design - File indexing, Java Model, type resolution, imports
Annotations - Strategy for constant evaluation (phased approach)
Integration - Entry points, test infrastructure, CLI integration
Testing - 3-phase strategy (unit → integration → existing suites)
Roadmap - 5 milestones with concrete tasks and success criteria
Limitations - Current scope and future work
Risks - Technical risks with mitigation strategies
Open Questions - Design decisions still pending
References - Key files and documentation
Appendix - Complete code examples

## corrections to the document:

Please apply the following corrections to the generated document:

3.4 Import Handling

Change naming of the imports storing parameters and related entities, to use the same terminology as in FIR. The `singleTypeImports` are names `simpleImports` in FIR, and `onDemandImports` are named `starImports`.

Also review other identifiers in the document, for the compatibility with compiler/FIR naming scheme. Propagate the naming changes through the rest of the document. 

Also drop the implementation time estimations. 

Do not forget to update document history.
