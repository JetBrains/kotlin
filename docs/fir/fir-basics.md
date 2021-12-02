# Short FIR overview

FIR (Frontend IR) compiler is a new Kotlin Frontend which has entirely new architecture compared to FE 1.0 (existing compiler frontend),
which brings a lot of improvements like

- much better performance
- clear contracts for different parts of the frontend
- convenient representation of user code, which better represents semantic of code

FIR tree is a core abstraction for the new frontend, and it contains all information that compiler new about the code. Compilation in the
FIR compiler happens in separate compiler phases. Those phases are executed sequentially in the CLI compiler and lazily in the Analysis API.
There is a guarantee that if we have a phase `A` and following phase  `B`, then all FIR elements which can be observed in phase `B` are
already resolved to phase `A`. This is a very important invariant that determines which information in FIR elements will be resolved on each
phase.

## Phases

There is the list of all FIR phases which exists in the compiler right now:

- **RAW_FIR**: In this phase, we ran translator from some parser AST (currently FIR supports two parser representations: PSI and LighterAst)
  to FIR tree. During conversion, FIR translator performs desugaring of the source code. This includes replacing all `if` expressions with
  corresponding `when` expressions, converting `for` loops into blocks with `iterator` variable declaration and `while` loop.
- **IMPORTS**: In this stage compiler resolves qualifiers of all imports, excluding the last part of an imported name (so if import
  is `import aaa.bbb.ccc.D` then the compiler tries to resolve `aaa.bbb.ccc` package)
- **ANNOTATIONS_FOR_PLUGINS** and **COMPANION_GENERATION**. Those phases are required for plugin support
- **SUPER_TYPES**: At this stage, the compiler resolves all supertypes of all non-local classes and expansions of type aliases
- **SEALED_CLASS_INHERITORS**: This phase is needed to collect and record all inheritors of non-local sealed classes
- **TYPES**: At this stage, the compiler resolves all other explicitly written types in declaration headers like
    - explicit return types of members
    - types of value parameters of functions
    - extension receivers of members
    - bounds of type parameters
    - types of annotations (without resolution of annotation arguments)
- **STATUS**: At this phase compiler resolves modality, visibility, and modifiers of all non-local declarations. Note that modality and
  modifiers of members (functions and properties) may depend on super declarations (if this member overrides some other member)
- **ARGUMENTS_OF_ANNOTATIONS**: here compiler resolves arguments of annotations in declaration headers
- **CONTRACTS**: This phase is needed for the resolution of contract block in functions and property accessors
- **IMPLICIT_TYPES_BODY_RESOLVE**: At this stage, the compiler resolves bodies of all functions and properties which have no explicit return
  type (`fun foo() = ...`)
- **BODY_RESOLVE**: At this stage, all other bodies are resolved
- **CHECKERS**: At this point, all FIR tree is already resolved and it's time to check it and report diagnostics for the user if needed.
  Note that it's allowed to report diagnostics only in this phase. If some diagnostic can be detected only during resolution (e.g. error
  that type of argument does not match with the expected type of parameter) then information about such errors is saved right inside FIR
  and converted to proper diagnostic only on **CHECKERS** stage
- **FIR2IR**: At this stage, the compiler transforms resolved FIR to backed IR

As you may notice, phases from `SUPER_TYPES` till `CONTRACTS` are run for non-local declarations. So when the compiler meets some local
classifier declaration (local class or anonymous object) during body resolve, it runs all those phases for that classifier specifically.

## FIR elements

All nodes of FIR tree are inheritors
of [FirElement](https://jetbrains.team/p/kt/repositories/kotlin/files/compiler/fir/tree/gen/org/jetbrains/kotlin/fir/FirElement.kt) class.
There are three main kinds of FirElement:

- [FirDeclaration](https://jetbrains.team/p/kt/repositories/kotlin/files/compiler/fir/tree/gen/org/jetbrains/kotlin/fir/declarations/FirDeclaration.kt)
  is a base class for all declaration nodes. Each declaration must have a unique `symbol` which identifies this declaration. Symbol is used
  to create references to declarations and also allows to recreate a declaration with the same symbol and doesn't break any reference to it
- [FirExpression](https://jetbrains.team/p/kt/repositories/kotlin/files/compiler/fir/tree/gen/org/jetbrains/kotlin/fir/expressions/FirExpression.kt)
  is a base class for all possible expressions that can be used in Kotlin code. All expressions have `typeRef` field with type of this
  specific expression
- [FirTypeRef](https://jetbrains.team/p/kt/repositories/kotlin/files/compiler/fir/tree/gen/org/jetbrains/kotlin/fir/types/FirTypeRef.kt) is
  a base class for any reference to the type in user code. There is a difference between a type and a reference to it in FIR. References to
  types are FIR elements which inherit `FirTypeRef` and actual types which are inheritors
  of [ConeKotlinType](https://jetbrains.team/p/kt/repositories/kotlin/files/compiler/fir/cones/src/org/jetbrains/kotlin/fir/types/ConeTypes.kt#L78) (
  similar to `KotlinType` from FE 1.0). There are three main kinds of `FirTypeRef`:
    - unresolved type refs (`FirUserTypeRef`) represent types which were explicitly declared in source code but not yet resolved to some
      specific cone type
    - implicit type refs (`FirImplicitTypeRef`) represent types which are not declared in code
      explicitly (`val x /*: FirImplicitTypeRef*/ = 1`)
    - resolved type refs (`FirResolvedTypeRef`) are resolved type refs which contain some specific cone type in `FirResolvedTypeRef.type`
      field

All node types (including leaf nodes) which are accessible from plugins are abstract and their implementations are hidden. So to create some
node you need to use special builder functions (which exist for every node) instead of calling a constructor of implementation:

```kotlin
val myFunction = buildSimpleFunction {
    name = Name.identifier("myFunction")
    ...
}
// instead of
val myFunction = FirSimpleFunctionImpl(
    name = Name.identifier("myFunction"),
    ...
)
```

There are no docs about all possible FirElements yet, but you can just explore them in compiler code. Most classes for FIR elements are auto
generated and written with explicitly declared all possible properties and methods so they are quite easy to understand.

## Providers

The main way to get some declaration inside compiler is
using [FirSymbolProvider](https://jetbrains.team/p/kt/repositories/kotlin/files/compiler/fir/providers/src/org/jetbrains/kotlin/fir/resolve/providers/FirSymbolProvider.kt#L28)
and [FirScope](https://jetbrains.team/p/kt/repositories/kotlin/files/compiler/fir/tree/src/org/jetbrains/kotlin/fir/scopes/FirScope.kt?&line=12)
.

_Symbol provider_ is used to lookup for classes by
their [ClassId](https://github.com/JetBrains/kotlin/blob/mastercore/compiler.common/src/org/jetbrains/kotlin/name/ClassId.java#L34) and
top-level functions and properties by
their [CallableId](https://jetbrains.team/p/kt/repositories/kotlin/files/core/compiler.common/src/org/jetbrains/kotlin/name/CallableId.kt).
Main symbol provider is a composition of multiple symbol providers, each of which looks up for declaration in specific scopes:

- In sources of current modules
- In generated (by plugins) declarations for the current module
- In dependent modules (if current module depends on sources of current module, which is possible in IDE)
- In binary dependencies

For callables composite symbol provider looks throw all scopes and returns a collection of symbols with specific callable id. For the
classifiers, there is a contract that there can not be more than one classifier with the same classId, so the composite provider looks for a
classifier symbol until it meets one.

_Scopes_ are used to looking for declarations in some specific scopes, e.g. in scope of imports in some file or declaration of some specific
class.

Note that scopes and providers return symbols of declarations, not declarations itself, and in most cases it's illegal to access FIR
declaration directly. So if you got some symbol for function from a symbol provider and want to know its return type, you need to access it
via the symbol itself, not from corresponding FIR. This is needed to provoke resolution of corresponding declaration to the required phase
in Analysis API mode, because in Analysis API all resolution is lazy.

```kotlin
val functionSymbol: FirNamedFunctionSymbol = ...
val returnType: FirResolvedTypeRef = functionSymbol.resolvedReturnTypeRef
// instead of
val returnType = functionSymbol.fir.returnTypeRef as FirResolvedTypeRef
```

Please don't forget to make sure that you are in compiler phase which can guarantee that some part of the declaration is resolved. For
example, it's illegal to access resolved return type of some function in `STATUS` stage, because at this point implicit return types are not
resolved (they require `IMPLICIT_TYPES_BODY_RESOLVE` phase)
