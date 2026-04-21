# Approaches to Java sources parsing and resolution

Investigating possibility of replacing `KotlinJavaPsiFacade` ([KT-70023](https://youtrack.jetbrains.com/issue/KT-70023/K2-consider-getting-rid-of-KotlinJavaPsiFacade)).

Meeting on 20.11.2025  
Participants:

- Ilya Chernikov  
- Denis Zharkov  
- Mikhail Glukhikh  
- Stanislav Erokhin  
- Dmitriy Novozhilov  
- Ivan Kochurkin

The decisions are listed at the end of the document.

## Motivation:

- It is the main obstacle on the way to getting rid of the IJ platform dependency ([OSIP-191](https://youtrack.jetbrains.com/issue/OSIP-191/Get-rid-of-IJ-platform-dependency-in-Compiler-Frontend))  
- it is designed for the IDE use case and therefore not optimal for the compiler use case  
  - PSI is heavy  
  - a lot of infrastructure scaffolding required  
- Light classes are a pain  
- Limited control over the processes happening inside the resolve  
  - declarations referenced from both Kotlin and Java code probably deserialized twice  
  - laziness/eagerness of the analysis  
- Keeping Java support up to date, as well as getting bugfixes, requires the platform update, which is a special pain now.

## Option 0: status quo

Leave everything as is, and continue suffering.

## What problems do we need to solve with a new facade

### Parsing

An easy one, multiple solutions are available.

### Model building

I.e. `JavaClass` and everything needed to build it, including references to types not represented in the same set of Java sources ("compilation unit") and annotation arguments (constant evaluation).

#### Types from classpath

It will be nice to avoid duplication of external type representation used in the Java model and in the Kotlin one.  
(Do we have this deduplication with our current PSI-based implementation? Most likely, not).

#### Types from Kotlin (K-J-K, "light classes")

If we delegate the resolve/analysis to some external system, like the PSI-based resolution we're using now, we need to provide some compatible representation of the Kotlin classes of the module we're currently compiling, which are referenced from the Java classes we're analyzing. This is named "light classes". (For FIR, the entry point is `FirJavaElementFinder`)

#### Name resolution in general

Pretty straightforward for Java? FQN \-\> class mapping \+ built-ins

#### Complex resolution and inference

So far irrelevant for our use case, but if Java decides to allow, e.g., inference for public declarations, things may become more difficult.

#### Annotation arguments (constants evaluation)

We need to be able to resolve annotation arguments, including constant evaluation, according to Java rules.

## Options beyond 0

### current javac-based implementation

- implemented using the undocumented internal API (`com.sun.tools.javac.main.JavaCompiler`, `com.sun.tools.javac.tree.JCTree`,)  
  - outdated, maybe problematic to bump JDK version  
  - official API is available, although generally the stability of it is not guaranteed anyway  
- only parsing is used  
  - technically there is the `compile` functionality too, but it is meant for a different use case and is not used for analysis  
- so, all analysis functionality, including constant evaluation, is implemented "by hand"  
- depends on some K1-specific code (e.g., `JavacBasedClassFinder`), although not in the "core" places

### ECJ-based (prototype)

- parser only so far; no analysis is implemented yet  
- it is possible to use ECJ analysis, but:  
  - to provide light classes, we either should have class file stubs or implement internal `INameEnvironment`, but the ECJ documentation strongly discourages it  
  - if not `INameEnvironment`, the problem of "duplicated deserialization" will remain  
- on the positive side:  
  - ECJ is designed for the embedded use case, so there will be fewer side effects, scaffolding, and other issues that we have now with the IJ platform  
  - actively developed, quickly supports new Java features; some important projects are based on it (VSCode's Java support, Eclipse itself)  
- some operational/moral issues  
  - difficult to justify the choice in front of the IJ team  
  - will likely result in somewhat negative opinions in the community  
  - will be difficult to ask ECJ devs for specific fixes, if needed

### official-javac-javac-API-based (re)implementation

- There is an official API to the javac (since 1.6 actually), [jdk.compiler module](https://docs.oracle.com/en/java/javase/24/docs/api/jdk.compiler/module-summary.html), [javax.tools.JavaCompiler](https://docs.oracle.com/en/java/javase/24/docs/api/java.compiler/javax/tools/JavaCompiler.html), [com.sun.source.tree AST representation](https://docs.oracle.com/en/java/javase/24/docs/api/jdk.compiler/com/sun/source/tree/package-summary.html), etc.  
- we can reimplement or repurpose the current implementation to simplify future migrations  
- parsing part is easy; it will not be much different from the current one  
- analysis:  
  - we can repurpose the analysis from the current javac-based implementation as is, but we have no idea how good it is in comparison to the correct analysis we have now from the IJ platform, and it is likely insufficient  
  - we don't know what it takes to develop it further in the current form  
  - alternatively, we can think about creating class file stubs from Kotlin code, providing them via the `FileManager` and using `javac` analysis  
- maybe the duplicated deserialization problem could be addressed via `FileManager` too  
- the obvious benefit of this solution is the Java support based on the official code  
- besides the technical difficulties listed above, we should check the licensing carefully, since we're dealing with Oracle here. ([Here](https://github.com/kohlschutter/jdk.compiler.standalone?tab=readme-ov-file) the author claims that it is GPLv2+Classpath-Exception, so maybe it is not immediately unsafe).  
- we should probably also check why the old implementation uses the unofficial API, and whether the used considerations are valid for our case now as well

### new custom implementation

- parsing \- we have several variants  
  - KMP Java parser from IJ platform `com.intellij.platform.syntax` \+ `com.intellij.java.syntax` (I have some raw prototype)  
    - in fact, do not depend on the IJ platform; separate published artefacts with minimal dependencies; no need to keep in sync with the platform version  
    - used in Fleet for web and is the base for the Kotlin KMP parser too  
    - actively developed, but in case of sunsetting, we can likely take it into our codebase without much friction  
  - [javax.tools.JavaCompiler](https://docs.oracle.com/en/java/javase/24/docs/api/java.compiler/javax/tools/JavaCompiler.html) could be used as a parser too  
  - I'm sure there are more, like something ANTLR based, etc.  
- analysis: we can try to reuse FIR analysis for it  
  - solves problems with light classes and duplicated deserialization by design  
  - we need to have (and maintain) separately configured "pipeline" for resolution and constant evaluation  
  - pro: most optimal and fully controlled pipeline  
  - cons: we need to support, albeit partially, the semantics of the second language in the compiler  
  - far-fetched dream: paves a way for a close interop with other PLs/platforms

## Implementation challenges

### Parsing

Not really a challenge; many external and JB-internal implementations are available.

### FIR-based analysis

- resolution rules for Java are moderately different from the Kotlin ones, so we have to maintain a separately configured resolution  
- but it is likely quite simple  
- constant evaluation \- we have to understand whether our current (former IR-based) evaluator is suitable

### Non-FIR Java model based analysis

E.g., as it is now implemented in the `javac-wrapper` module. I'm not sure that we should consider it, because it looks like a least-beneficial variant, with no help neither from an existing implementation nor from the FIR infrastructure.

### Using external implementation of analysis

Most likely javac-based, since it looks like the ECJ variant is mostly inferior. In any case, we need to solve the problem of "light classes", and we potentially want to solve duplicated deserialization, although we can probably live without it.

#### Generating classfile stubs

This solution should work both for `javac` and ECJ. It should probably be doable, since we're building PSI stubs now just fine, so building ones with `asm` should be feasible too.

#### Providing dependencies

We do not want to just pass the classpath and files to the external analyzer and rely on its own deserialization and search logic, because it could be quite a big waste of resources. For `javac` we have the possibility, at least, to pass binary class files via some `FileManager` implementation, but deserialization will likely be inaccessible for us. For ECJ, we have the theoretical possibility to implement an internal `INameEnvironment`, but it is explicitly discouraged by ECJ developers, so it is probably not a good idea.

### Hybrid/staged analysis

Theoretically we can try to run external analysis in a "resilient" mode and then try to resolve "error elements" ourselves. But most likely it will be as difficult as having our own analysis and also lead to inconsistencies

## Non-implementational challenges

### Maintaining compatibility with the chosen external subsystem

Looks relatively easy for all mentioned variants, except for the current solution

### Maintaining compatibility with Java development

In the case of our own analysis implementation, we will need to keep track of Java development. But so far they are quite conservative about the parts that we care about, namely visible declarations.

### External communications and bugfixing

Likely a problem for the ECJ case

### Licensing

It could be or could become a problem with `javac`\-based solutions.

## Decision process

We need to analyze the costs of the challenges that we may face and make the decision based on the costs.

## Decisions made at the meeting on 20.11.2025

Go for the new custom implementation prototype:

* The reasons mentioned in this document were basically accepted. In addition, the following arguments were given:  
  \+ custom: maintenance in the long run will likely be the lowest among other variants  
  \+ custom: full control over the pipeline  
  \- javac: compatibility with all versions  
  \- custom: big risk (of very low probability) if expressions resolution is suddenly required for java declarations resolution. But in this or similar cases we likely will have enough time to solve the problems.  
  \- javac: big risk (of low to moderate probability) that the future implementations may get non-permissive licensing  
  \- javac (\&ecj) "light classes" are likely difficult to support for them  
*  The technical details arguments converged to the following points:  
  * Annotation arguments are important, and probably most unpredictable applications may come from plugins. But the complexity is rather limited. And maybe we don't need to reuse anything from the Kotlin implementation.  
  * We should try first to implement another java model, instead of generating FIR  
    * we need laziness  
    * since we will continue to use java model for binary classes, it makes sense to use it for the source classes too, to reduce amount of possible discrepancies  
    * behind the java model we can try to reuse some FIR infrastructure "in the air", if it will simplify the implementation.  
      * FIR type resolver is more or less ready for such use  
      * there are some difficulties around supertypes  
      * there should be some special filters, e.g. for type aliases  
      * we can try an approach of building FIR where it is beneficial, but make sure that we access it from the rest of the code only via java model  
  * use KMP parser (org.jetbrains.java.syntax.jvm)

The full video (in Russian) is [here](https://drive.google.com/file/d/1lmNAY42DMzscF2ai98xUpQ6F5EnOdDjM/view)  
