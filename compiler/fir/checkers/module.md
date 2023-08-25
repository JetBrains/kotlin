# FIR Checkers

## Checkers structure

There are four kinds of checkers:
- [DeclarationChecker](./src/org/jetbrains/kotlin/fir/analysis/checkers/declaration/FirDeclarationChecker.kt)
- [ExpressionChecker](./src/org/jetbrains/kotlin/fir/analysis/checkers/expression/FirExpressionChecker.kt)
- [FirTypeChecker](./src/org/jetbrains/kotlin/fir/analysis/checkers/type/FirTypeChecker.kt)
- [FirControlFlowChecker](./src/org/jetbrains/kotlin/fir/analysis/checkers/cfa/FirControlFlowChecker.kt)

The first three kinds are typed and may be restricted to checking only a specific type of declaration/expression/type ref. To simplify working with checkers for different FIR elements, there is a number of typed typealiases:
- Declarations: [FirDeclarationCheckerAliases.kt](./gen/org/jetbrains/kotlin/fir/analysis/checkers/declaration/FirDeclarationCheckerAliases.kt)
- Expressions: [FirExpressionCheckerAliases.kt](./gen/org/jetbrains/kotlin/fir/analysis/checkers/expression/FirExpressionCheckerAliases.kt)
- Type refs: [FirTypeCheckerAliases.kt](./gen/org/jetbrains/kotlin/fir/analysis/checkers/type/FirTypeCheckerAliases.kt)

The last kind of checker, `FirControlFlowChecker`, is for checkers which perform Control Flow Analysis (CFA) and is supposed to work with every declaration that has its own Control Flow Graph (CFG)

## Checkers contracts

All checkers are supposed to satisfy the following contracts:
1. checkers are stateless
2. checkers are independent
3. checkers are as specific as possible
4. checkers should try to avoid traversing the subtree of the element it checks
5. checkers should not rely on the syntax

Those contracts imply the following:
1. Usually a checker is an `object` without any state
2. Each checker should work correctly even if all other checkers are disabled
3. If a checker is meant to check only simple functions, there is no need to parameterize it with `FirDeclaration` and check if the declaration is a `FirSimpleFunction`. Just parameterize the checker itself with `FirSimpleFunction`
    - this is needed not only for simplification of code, but also for the sake of performance. Typed checkers are run only on elements with a suitable type. So if you declared a `FirRegularClassChecker` it will never be run for a `FirAnonymousObject`
4. If a checker is supposed to check anonymous initializers, it's better to create a `FirAnonymousInitializerChecker` which will be separately run for each `init` block in the class rather than creating a `FirClassChecker` which will manually iterate over each `init` block in this class. There are several reasons for that:
    - the diagnostic suppression mechanism is implemented in the checkers dispatcher, so reporting something on a sub-element may cause false-positive diagnostics, if there was a `@Suppress` annotation between the root element (passed to the checker) and the sub-element. There is a mechanism to fix it, but it's not recommended to use
    - checkers with smaller scope increase IDE performance because they require fewer elements to be resolved in order to check something
5. FIR compiler is made syntax-agnostic and can work with different parsers and syntax tree (at this moment it already supports PSI and LightTree syntax trees), so checkers should not rely on any syntax implementation details. Instead of that, checkers should use [positioning strategies](../../frontend.common-psi/src/org/jetbrains/kotlin/diagnostics/SourceElementPositioningStrategies.kt) to more precise positioning of diagnostics for specific elements (e.g. it allows to render diagnostic on class name using the source of the whole class). The only exception from this rule are inheritors of [FirSyntaxChecker](./src/org/jetbrains/kotlin/fir/analysis/checkers/syntax/FirSyntaxChecker.kt), which work directly with a syntax tree (and must support several implementations for different ASTs)

## Checkers pipeline

All checkers are collected in special containers, named [DeclarationCheckers](./gen/org/jetbrains/kotlin/fir/analysis/checkers/declaration/DeclarationCheckers.kt), [ExpressionCheckers](./gen/org/jetbrains/kotlin/fir/analysis/checkers/expression/ExpressionCheckers.kt) and [TypeCheckers](./gen/org/jetbrains/kotlin/fir/analysis/checkers/type/TypeCheckers.kt). Those containers have fields with sets of checkers for each possible type of checker of corresponding kind

There is a number of different container groups:
- Common checkers, which always run on any platform
    - [CommonDeclarationCheckers](./src/org/jetbrains/kotlin/fir/analysis/checkers/CommonDeclarationCheckers.kt)
    - [CommonExpressionCheckers](./src/org/jetbrains/kotlin/fir/analysis/checkers/CommonExpressionCheckers.kt)
    - [CommonTypeCheckers](./src/org/jetbrains/kotlin/fir/analysis/checkers/CommonTypeCheckers.kt)
- Checkers for each specific platform (lay in the corresponding `:compiler:fir:checkers:checkers.platform` modules)
    - JVM:
        - [JvmDeclarationCheckers](./checkers.jvm/src/org/jetbrains/kotlin/fir/analysis/jvm/checkers/JvmDeclarationCheckers.kt)
        - [JvmExpressionCheckers](./checkers.jvm/src/org/jetbrains/kotlin/fir/analysis/jvm/checkers/JvmExpressionCheckers.kt)
        - [JvmTypeCheckers](./checkers.jvm/src/org/jetbrains/kotlin/fir/analysis/jvm/checkers/JvmTypeCheckers.kt)
    - JS:
        - [JsDeclarationCheckers](./checkers.js/src/org/jetbrains/kotlin/fir/analysis/js/checkers/JsDeclarationCheckers.kt)
        - [JsExpressionCheckers](./checkers.js/src/org/jetbrains/kotlin/fir/analysis/js/checkers/JsExpressionCheckers.kt)
    - Native:
        - [NativeDeclarationCheckers](./checkers.native/src/org/jetbrains/kotlin/fir/analysis/native/checkers/NativeDeclarationCheckers.kt)
        - [NativeExpressionCheckers](./checkers.native/src/org/jetbrains/kotlin/fir/analysis/native/checkers/NativeExpressionCheckers.kt)
- Extended checkers. Those checkers are disabled by default and can be enabled with the `-Xuse-fir-extended-checkers` compiler flag. This group includes experimental and not very performant checkers, which are not crucial for regular compilation
    - [ExtendedDeclarationCheckers](./src/org/jetbrains/kotlin/fir/analysis/checkers/ExtendedDeclarationCheckers.kt)
    - [ExtendedExpressionCheckers](./src/org/jetbrains/kotlin/fir/analysis/checkers/ExtendedExpressionCheckers.kt)
    - [ExtendedTypeCheckers](./src/org/jetbrains/kotlin/fir/analysis/checkers/ExtendedTypeCheckers.kt)

At the beginning of the compilation, in the initialization phase, all required checker containers are collected inside a session component named [CheckersComponent](./src/org/jetbrains/kotlin/fir/analysis/CheckersComponent.kt). When the time of checker phase comes, the compiler [creates](https://github.com/JetBrains/kotlin/blob/master/compiler/fir/entrypoint/src/org/jetbrains/kotlin/fir/pipeline/analyse.kt#L23) an instance of [AbstractDiagnosticCollector](./src/org/jetbrains/kotlin/fir/analysis/collectors/AbstractDiagnosticCollector.kt), which is responsible to run all checkers. `DiagnosticCollector` traverses the whole given FIR tree, collects `CheckerContext` during this traversal, and runs all checkers that suite the element type on each element.

## Checker Context

[CheckerContext](./src/org/jetbrains/kotlin/fir/analysis/checkers/context/CheckerContext.kt) contains all information which can be used by checkers, including
- `session` and `scopeSession`
- the list of `containingDeclarations`
- various information about the body which is analyzed
- the stack of implicit receivers
- information about suppressed diagnostics

`CheckerContext` is meant to be read-only for checkers

## Diagnostic reporting

All diagnostics which can be reported by the compiler are stored within the [FirErrors](./gen/org/jetbrains/kotlin/fir/analysis/diagnostics/FirErrors.kt), [FirJvmErrors](./checkers.jvm/gen/org/jetbrains/kotlin/fir/analysis/diagnostics/jvm/FirJvmErrors.kt), [FirJsErrors](./checkers.js/gen/org/jetbrains/kotlin/fir/analysis/diagnostics/js/FirJsErrors.kt) and [FirNativeErrors](./checkers.native/gen/org/jetbrains/kotlin/fir/analysis/diagnostics/native/FirNativeErrors.kt) objects. Those diagnostics are auto-generated based on the diagnostic description in one of a diagnostic list in [checkers-component-generator](./checkers-component-generator/src/org/jetbrains/kotlin/fir/checkers/generator/diagnostics).

The generation is needed, because Analysis API (AA), which is used in IDE, generates a separate class for each compiler diagnostic with proper conversions of arguments for parametrized diagnostics. And the goal of the code generator is to automatically generate those classes and conversions. To run the diagnostics generation use the `Generators -> Generate FIR Checker Components and FIR/IDE Diagnostics` run configuration.

Diagnostic messages must be added manually to [FirErrorsDefaultMessages](./src/org/jetbrains/kotlin/fir/analysis/diagnostics/FirErrorsDefaultMessages.kt), [FirJvmErrorsDefaultMessages](./checkers.jvm/src/org/jetbrains/kotlin/fir/analysis/diagnostics/jvm/FirJvmErrorsDefaultMessages.kt), [FirJsErrorsDefaultMessages](./checkers.js/src/org/jetbrains/kotlin/fir/analysis/diagnostics/js/FirJsErrorsDefaultMessages.kt) and [FirNativeErrorsDefaultMessages](./checkers.native/src/org/jetbrains/kotlin/fir/analysis/diagnostics/native/FirNativeErrorsDefaultMessages.kt) respectively. Guidelines for diagnostic messages are described in the header of `FirErrorsDefaultMessages`

To report diagnostics, each checker takes an instance of [DiagnosticReporter](../../frontend.common/src/org/jetbrains/kotlin/diagnostics/DiagnosticReporter.kt) as a parameter. To reduce the boilerplate needed to instantiate a diagnostic from the given factory and ensure it's not missed due to reporting on the null source, a one should use the utilities from [KtDiagnosticReportHelpers](../../frontend.common/src/org/jetbrains/kotlin/diagnostics/KtDiagnosticReportHelpers.kt)

## FIR contracts at checker stage

In CLI mode the compiler runs checkers only after it has analyzed the whole world up to the final FIR phase (`BODY_RESOLVE`). But the IDE uses lazy resolve, so there can be a situation when some files have been analyzed to `BODY_RESOLVE` and other files have not been analyzed at all. This means that in a checker one can not rely on the fact that some FIR elements should have been resolved to some specific phase. The only exception is the following: **If some element was passed directly to the checker then it is guaranteed that this element is already resolved to the `BODY_RESOLVE` phase**. If some declaration is received somewhere from outside (from a type, a symbol provider or a scope), then it could have been resolved up to an arbitrary phase.

So, to avoid possible problems with accessing some information from FIR elements which was not yet calculated in the AA mode, there are the following restrictions and recommendations:
- Access to `FirBasedSymbol<*>.fir` is prohibited. One can not extract any FIR element from the corresponding symbol
- Instead of that, if some information about the declaration is needed (e.g., the list of supertypes for some class symbol), special accessors from that symbol should be used (they are declared as members of symbols). Those accessors call lazy resolution to the least required phase and after that extract the required information from FIR

## Resolution diagnostics

While all checkers are run after resolution of the code is finished, some diagnostics can be actually detected only during resolution, such as
- inference errors (type mismatch, no information for type parameter)
- call resolution errors (overload resolution ambiguity)
- type resolution errors (cycle in supertypes)
- visibility errors (invisible reference)
- etc

And at the same time, there is a contract that FIR resolution is side effect free (not very formal but still) and produces only a resolved FIR tree. So diagnostics can not be reported from resolution directly.  

To support such diagnostics, there is the following mechanism:
- some FIR nodes (mostly with word `Error` in name, like [FirResolvedErrorReference](../tree/gen/org/jetbrains/kotlin/fir/references/FirResolvedErrorReference.kt)) have a property which contain a `ConeDiagnostic`
- [ConeDiagnostic](../cones/src/org/jetbrains/kotlin/fir/diagnostics/ConeDiagnostic.kt) is an indicator that something went wrong during resolution
  - there are a lot of different kinds of `ConeDiagnostic` for any possible problems, see [ConeDiagnostics.kt](../semantics/src/org/jetbrains/kotlin/fir/resolve/diagnostics/ConeDiagnostics.kt)
- `ConeDiagnostic` is saved in the FIR tree, and then the special checker component ([ErrorNodeDiagnosticCollectorComponent](./src/org/jetbrains/kotlin/fir/analysis/collectors/components/ErrorNodeDiagnosticCollectorComponent.kt)) checks all FIR nodes and report proper diagnostics based on the found `ConeDiagnostic` 
