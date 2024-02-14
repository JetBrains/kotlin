# Short FIR overview

FIR (Frontend IR) compiler is a new Kotlin Frontend that has entirely new architecture compared to FE1.0 (existing compiler frontend) and brings a lot of improvements:
- much better performance;
- clear contracts for different parts of the frontend;
- convenient representation of user code, which better represents the semantics of code.

FIR tree is a core abstraction for the new frontend. FIR tree contains all information the compiler knows about the code. Compilation in the
FIR compiler is performed in separate compiler phases. Compiler phases are executed sequentially in the CLI compiler and lazily in the Analysis API.
There is a guarantee that if we have two phases: `A` and `B` where `B` follows `A`, then all FIR elements visible in phase `B` are already resolved to phase `A`. 
This is a crucial invariant that determines which information in FIR elements is resolved in each phase.
See [FirResolvePhase](../../compiler/fir/tree/src/org/jetbrains/kotlin/fir/declarations/FirResolvePhase.kt) for more details and up-to-date information regarding the compiler phases.

## Phases
List of all FIR phases that exist in the compiler right now with a short description:
- **RAW_FIR**: We ran the translator from some parser AST to FIR tree.
- **IMPORTS**: The compiler splits all imports on longest existing package part and related class qualifier.
- **COMPILER_REQUIRED_ANNOTATIONS**: The compiler resolves types of some specific compiler annotations which are required earlier than the **TYPES** phase.
- **COMPANION_GENERATION**. The compiler generates companion objects which were provided by compiler plugins
- **SUPER_TYPES**: The compiler resolves all supertypes of classes and performs type aliases expansion. 
- **SEALED_CLASS_INHERITORS**: The compiler collects and records all inheritors of sealed classes.
- **TYPES**: The compiler resolves all other explicitly written types in declaration headers.
- **STATUS**: The compiler resolves modality, visibility, and modifiers for member declarations.
- **EXPECT_ACTUAL_MATCHING**: The compiler matches and records an `expect` member declaration for `actual` member declarations.
- **CONTRACTS**: The compiler resolves a contract definition in property accessors, functions, and constructors.
- **IMPLICIT_TYPES_BODY_RESOLVE**: The compiler resolves types for callable declarations without an explicit return type.
- **ANNOTATION_ARGUMENTS**: The compiler resolves arguments of annotations in declaration headers.
- **BODY_RESOLVE**: The compiler resolves bodies for declarations.
- **CHECKERS**: At this point, all FIR tree is already resolved, and it's time to check it and report diagnostics for the user.
  Note that it's allowed to report diagnostics only in this phase. If some diagnostic can be detected only during resolution (e.g, error
  that type of argument does not match with the expected type of parameter) then information about such errors is saved right inside the FIR tree
  and converted to proper diagnostic only on the **CHECKERS** stage.

- **FIR2IR**: At this stage, the compiler transforms resolved FIR to backed IR.

As you may notice, phases from `COMPILER_REQUIRED_ANNOTATIONS` till `ANNOTATION_ARGUMENTS` run for non-local declarations.
When the compiler meets some local classifier declaration (local class or anonymous object) during body resolve,
it runs all those phases for that classifier specifically.

## FIR elements

All nodes of the FIR tree are inheritors
of [FirElement](https://github.com/JetBrains/kotlin/blob/master/compiler/fir/tree/gen/org/jetbrains/kotlin/fir/FirElement.kt) class.
There are three main kinds of FirElement:

- [FirDeclaration](https://github.com/JetBrains/kotlin/blob/master/compiler/fir/tree/gen/org/jetbrains/kotlin/fir/declarations/FirDeclaration.kt) is a base class for all declaration nodes. Each declaration must have a unique symbol identifying this declaration. The symbol is used to create declaration references and allows to recreate a declaration with the same symbol and don't break any reference to it.
- [FirExpression](https://github.com/JetBrains/kotlin/blob/master/compiler/fir/tree/gen/org/jetbrains/kotlin/fir/expressions/FirExpression.kt) is a base class for all possible expressions that can be used in the Kotlin code. All expressions have `typeRef` field containing the type of this
  specific expression.
- [FirTypeRef](https://github.com/JetBrains/kotlin/blob/master/compiler/fir/tree/gen/org/jetbrains/kotlin/fir/types/FirTypeRef.kt) is a base class for any reference to the type in the user code. There is a difference between a type and a type reference in FIR. Type references are FIR elements inheriting `FirTypeRef` and contain actual ConeKotlinType..
  of [ConeKotlinType](https://github.com/JetBrains/kotlin/blob/master/compiler/fir/cones/src/org/jetbrains/kotlin/fir/types/ConeTypes.kt) is a similar concept to `KotlinType` from FE1.0. There are three main kinds of `FirTypeRef`:
    - unresolved type refs (`FirUserTypeRef`) represent types refs explicitly declared in source code but not yet resolved to a specific `ConeKotlinType`;
    - implicit type refs (`FirImplicitTypeRef`) represent types refs not declared in code explicitly (`val x /*: FirImplicitTypeRef*/ = 1`);
    - resolved type refs (`FirResolvedTypeRef`) represent resolved type refs containing some specific cone type in `FirResolvedTypeRef.type`
      field.

All node types (including leaf nodes) accessible from plugins are abstract and their implementations are hidden. To create some
node you need to use special builder functions (one exist for every node) instead of calling a constructor of implementation:

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

There are no docs about all possible FirElements yet, but you can explore them in compiler code. Most classes for FIR elements are auto
generated and written with all possible members explicitly declared, so they are easy to understand.

## Providers

The main way to get some declaration inside compiler is [FirSymbolProvider](https://github.com/JetBrains/kotlin/blob/master/compiler/fir/providers/src/org/jetbrains/kotlin/fir/resolve/providers/FirSymbolProvider.kt)
and [FirScope](https://github.com/JetBrains/kotlin/blob/master/compiler/fir/tree/src/org/jetbrains/kotlin/fir/scopes/FirScope.kt).

_Symbol provider_ is used to lookup for classes by their [ClassId](https://github.com/JetBrains/kotlin/blob/mastercore/compiler.common/src/org/jetbrains/kotlin/name/ClassId.java) and top-level functions and properties by
their [CallableId](https://github.com/JetBrains/kotlin/blob/master/core/compiler.common/src/org/jetbrains/kotlin/name/CallableId.kt).
The main symbol provider is a composition of multiple symbol providers, each of them looks up for declaration in specific scopes:
- In sources of current modules,
- In generated declarations by plugins for the current module,
- In dependent source modules (for Analysis API)
- In binary dependencies.

For callables, the composite symbol provider looks through all scopes and returns a collection of symbols with a specific callable id. For the
classifiers, there is a contract stating that there can not be more than one classifier with the same ClassId, so the composite provider looks for a
classifier symbol until it meets one.

_Scopes_ are used to looking for declarations in some specific places, e.g. in file imports or class members.

Note that scopes and providers return symbols of declarations, not declarations themselves. In most cases, it's illegal to access FIR
declaration directly. Suppose you got s function symbol you got by a symbol provider. To get the function return type, you need to access it
via the symbol itself, not via the corresponding FIR element. Such contract is required to provoke resolution of the corresponding declaration in Analysis API mode as in Analysis API all resolution is lazy.

```kotlin
val functionSymbol: FirNamedFunctionSymbol = ...
val returnType: FirResolvedTypeRef = functionSymbol.resolvedReturnTypeRef
// instead of
val returnType = functionSymbol.fir.returnTypeRef as FirResolvedTypeRef
```

Please don't forget to make sure that you are in the correct compiler phase which can guarantee that required declaration parts are resolved. For example, it's illegal to access resolved return type of some function in `STATUS` stage, because at this point of time implicit return types are not
resolved. Implicit types are resolved at `IMPLICIT_TYPES_BODY_RESOLVE` phase.