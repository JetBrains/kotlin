# FIR Checkers

## Checkers structure

There are five kinds of checkers:
- [DeclarationChecker](./src/org/jetbrains/kotlin/fir/analysis/checkers/declaration/FirDeclarationChecker.kt)
- [ExpressionChecker](./src/org/jetbrains/kotlin/fir/analysis/checkers/expression/FirExpressionChecker.kt)
- [FirTypeChecker](./src/org/jetbrains/kotlin/fir/analysis/checkers/type/FirTypeChecker.kt)
- [FirLanguageVersionSettingsChecker](./src/org/jetbrains/kotlin/fir/analysis/checkers/config/FirLanguageVersionSettingsChecker.kt)
- [FirControlFlowChecker](./src/org/jetbrains/kotlin/fir/analysis/checkers/cfa/FirControlFlowChecker.kt)

The first three kinds are typed and may be restricted to checking only a specific type of declaration, expression, or type reference. To simplify working with checkers for different FIR elements, there are several typed typealiases:
- Declarations: [FirDeclarationCheckerAliases.kt](./gen/org/jetbrains/kotlin/fir/analysis/checkers/declaration/FirDeclarationCheckerAliases.kt)
- Expressions: [FirExpressionCheckerAliases.kt](./gen/org/jetbrains/kotlin/fir/analysis/checkers/expression/FirExpressionCheckerAliases.kt)
- Type refs: [FirTypeCheckerAliases.kt](./gen/org/jetbrains/kotlin/fir/analysis/checkers/type/FirTypeCheckerAliases.kt)

The next kind, `FirLanguageVersionSettingsChecker`, is used to check language version settings independently of particular code pieces.

The last kind of checker, `FirControlFlowChecker`, is for checkers that perform Control Flow Analysis (CFA) and is supposed to work with every declaration that has its own Control Flow Graph (CFG).

## Checkers contracts

All checkers are expected to satisfy the following contracts:
1. Checkers must be stateless.
2. Checkers must be independent.
3. Checkers must be as specific as possible.
4. Checkers should aim to avoid traversing the subtree of the element they check.
5. Checkers should not rely on the syntax.

These contracts imply the following:
1. Usually, a checker is an `object` without any state.
2. Each checker should work correctly even if all other checkers are disabled.
3. If a checker is meant to check only simple functions, there is no need to parameterize it with `FirDeclaration` and check if the declaration is a `FirSimpleFunction`. Instead, parameterize the checker itself with `FirSimpleFunction`.
    - This is necessary not only to simplify the code but also to improve performance. Typed checkers are run only on elements with a suitable type. For example, if you declare a `FirRegularClassChecker`, it will never be run for a `FirAnonymousObject`.
4. If a checker is supposed to check anonymous initializers, it's better to create a `FirAnonymousInitializerChecker` that is separately run for each `init` block in the class, rather than creating a `FirClassChecker` that manually iterates over each `init` block in the class. There are several reasons for this:
    - The diagnostic suppression mechanism is implemented in the checkers dispatcher, so reporting something on a sub-element can cause false-positive diagnostics if there is a `@Suppress` annotation between the root element (passed to the checker) and the sub-element. While there is a mechanism to fix this, it is not recommended to use it.
    - Checkers with a smaller scope improve IDE performance because they require fewer elements to be resolved in order to perform checks.
5. The FIR compiler is designed to be syntax-agnostic and can work with different parsers and syntax trees (at present, it already supports PSI and LightTree syntax trees). Therefore, checkers should not rely on any syntax implementation details. Instead, checkers should use [positioning strategies](../../frontend.common-psi/src/org/jetbrains/kotlin/diagnostics/SourceElementPositioningStrategies.kt) for more precise positioning of diagnostics for specific elements (e.g., this allows diagnostics to be rendered on a class name while using the source of the entire class). The only exception to this rule is inheritors of [FirSyntaxChecker](./src/org/jetbrains/kotlin/fir/analysis/checkers/syntax/FirSyntaxChecker.kt), which work directly with a syntax tree and must support several implementations for different ASTs.

## Checkers pipeline

All checkers are collected in special containers named [DeclarationCheckers](./gen/org/jetbrains/kotlin/fir/analysis/checkers/declaration/DeclarationCheckers.kt), [ExpressionCheckers](./gen/org/jetbrains/kotlin/fir/analysis/checkers/expression/ExpressionCheckers.kt), and [TypeCheckers](./gen/org/jetbrains/kotlin/fir/analysis/checkers/type/TypeCheckers.kt). These containers have fields with sets of checkers for each possible type of checker of the corresponding kind.

There are several different container groups:
- Common checkers, which always run on any platform:
    - [CommonDeclarationCheckers](./src/org/jetbrains/kotlin/fir/analysis/checkers/CommonDeclarationCheckers.kt)
    - [CommonExpressionCheckers](./src/org/jetbrains/kotlin/fir/analysis/checkers/CommonExpressionCheckers.kt)
    - [CommonTypeCheckers](./src/org/jetbrains/kotlin/fir/analysis/checkers/CommonTypeCheckers.kt)
- Checkers for a specific platform (located in the corresponding `:compiler:fir:checkers:checkers.platform` modules):
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
- Extra checkers: These checkers are disabled by default and can be enabled with the `-Wextra` compiler flag. This group includes less performant checkers that are not crucial for regular compilation.
    - [ExtraDeclarationCheckers](./src/org/jetbrains/kotlin/fir/analysis/checkers/ExtraDeclarationCheckers.kt)
    - [ExtraExpressionCheckers](./src/org/jetbrains/kotlin/fir/analysis/checkers/ExtraExpressionCheckers.kt)
    - [ExtraTypeCheckers](./src/org/jetbrains/kotlin/fir/analysis/checkers/ExtraTypeCheckers.kt)
- Experimental checkers: These checkers are disabled by default and can be enabled with the `-Xuse-fir-experimental-checkers` compiler flag. This group includes experimental checkers and exists to support the development of checkers that are not yet production-ready.
    - [ExtraDeclarationCheckers](./src/org/jetbrains/kotlin/fir/analysis/checkers/ExtraDeclarationCheckers.kt)
    - [ExtraExpressionCheckers](./src/org/jetbrains/kotlin/fir/analysis/checkers/ExtraExpressionCheckers.kt)
    - [ExtraTypeCheckers](./src/org/jetbrains/kotlin/fir/analysis/checkers/ExtraTypeCheckers.kt)

At the beginning of compilation, during the initialization phase, all required checker containers are collected in a session component named [CheckersComponent](./src/org/jetbrains/kotlin/fir/analysis/CheckersComponent.kt). When the checker phase starts, the compiler [creates](https://github.com/JetBrains/kotlin/blob/master/compiler/fir/entrypoint/src/org/jetbrains/kotlin/fir/pipeline/analyse.kt#L23) an instance of [AbstractDiagnosticCollector](./src/org/jetbrains/kotlin/fir/analysis/collectors/AbstractDiagnosticCollector.kt), which is responsible for running all checkers. The `DiagnosticCollector` traverses the entire FIR tree, collects `CheckerContext` during the traversal, and runs all checkers that match the element type on each element.

## Checker Context

[CheckerContext](./src/org/jetbrains/kotlin/fir/analysis/checkers/context/CheckerContext.kt) contains all the information that checkers can use, including:
- `session` and `scopeSession`
- the list of `containingDeclarations`
- various details about the body being analyzed
- the stack of implicit receivers
- information about suppressed diagnostics

`CheckerContext` is designed to be read-only for checkers.

## Diagnostic reporting

All diagnostics that can be reported by the compiler are stored within the [FirErrors](./gen/org/jetbrains/kotlin/fir/analysis/diagnostics/FirErrors.kt), [FirJvmErrors](./checkers.jvm/gen/org/jetbrains/kotlin/fir/analysis/diagnostics/jvm/FirJvmErrors.kt), [FirJsErrors](./checkers.js/gen/org/jetbrains/kotlin/fir/analysis/diagnostics/js/FirJsErrors.kt), and [FirNativeErrors](./checkers.native/gen/org/jetbrains/kotlin/fir/analysis/diagnostics/native/FirNativeErrors.kt) objects. These diagnostics are auto-generated based on diagnostic descriptions in one of the diagnostic lists in [checkers-component-generator](./checkers-component-generator/src/org/jetbrains/kotlin/fir/checkers/generator/diagnostics).

This generation process is necessary because the Analysis API (AA), which is used in the IDE, generates a separate class for each compiler diagnostic with proper conversions of arguments for parameterized diagnostics. The goal of the code generator is to automate the creation of these classes and conversions. To run diagnostic generation, use the `Generators -> Generate FIR Checker Components and FIR/IDE Diagnostics` run configuration.

Diagnostic messages must be added manually to [FirErrorsDefaultMessages](./src/org/jetbrains/kotlin/fir/analysis/diagnostics/FirErrorsDefaultMessages.kt), [FirJvmErrorsDefaultMessages](./checkers.jvm/src/org/jetbrains/kotlin/fir/analysis/diagnostics/jvm/FirJvmErrorsDefaultMessages.kt), [FirJsErrorsDefaultMessages](./checkers.js/src/org/jetbrains/kotlin/fir/analysis/diagnostics/js/FirJsErrorsDefaultMessages.kt), and [FirNativeErrorsDefaultMessages](./checkers.native/src/org/jetbrains/kotlin/fir/analysis/diagnostics/native/FirNativeErrorsDefaultMessages.kt) respectively. Guidelines for writing diagnostic messages are described in the header of `FirErrorsDefaultMessages`.

To report diagnostics, each checker takes an instance of [DiagnosticReporter](../../frontend.common/src/org/jetbrains/kotlin/diagnostics/DiagnosticReporter.kt) as a parameter. To reduce the boilerplate needed to instantiate a diagnostic from the given factory and ensure it’s not missed due to reporting on the null source, one should use utilities from [KtDiagnosticReportHelpers](../../frontend.common/src/org/jetbrains/kotlin/diagnostics/KtDiagnosticReportHelpers.kt).

## FIR contracts at the checker stage

In CLI mode, the compiler runs checkers only after analyzing the entire project up to the final FIR phase (`BODY_RESOLVE`). However, the IDE uses lazy resolve, meaning some files may have been analyzed to `BODY_RESOLVE` while others may not have been analyzed at all. This means that in a checker one cannot rely on the fact that some FIR elements should have been resolved to some specific phase. The only exception is the following: **If an element is passed directly to the checker, it is guaranteed that the element has been resolved to the `BODY_RESOLVE` phase.** However, if a declaration is retrieved from an external source (e.g., a type, symbol provider, or scope), it may have been resolved to an arbitrary phase.

To handle this and avoid issues with accessing data from FIR elements that haven’t been calculated yet in the AA mode, the following restrictions and recommendations apply:
- Access to `FirBasedSymbol<*>.fir` is prohibited. Checkers are not allowed to extract any FIR element from the corresponding symbol.
- Instead, if information about a declaration is needed (e.g., the list of supertypes for a class symbol), dedicated accessors from that symbol should be used. These accessors are declared as members of symbols, perform lazy resolution to the minimum required phase, and then extract the relevant information from FIR.

## Resolution diagnostics

While all checkers are executed after code resolution is finished, some diagnostics can only be detected during resolution. These include:
- Inference errors (e.g., type mismatch, missing type parameter information)
- Call resolution errors (e.g., overload resolution ambiguity)
- Type resolution errors (e.g., cycles in supertypes)
- Visibility errors (e.g., invisible references)
- And others

At the same time, FIR resolution adheres to the principle of being side-effect-free (informally) and produces only a resolved FIR tree. As a result, diagnostics cannot be reported during resolution directly.

To handle such diagnostics, the following mechanism is in place:
- Certain FIR nodes, mostly those with the word `Error` in their name (e.g., [FirResolvedErrorReference](../tree/gen/org/jetbrains/kotlin/fir/references/FirResolvedErrorReference.kt)), include a property that contains a `ConeDiagnostic`.
- [ConeDiagnostic](../cones/src/org/jetbrains/kotlin/fir/diagnostics/ConeDiagnostic.kt) is an indicator that something went wrong during resolution.
    - There are many types of `ConeDiagnostic` to represent different possible issues. Refer to [ConeDiagnostics.kt](../semantics/src/org/jetbrains/kotlin/fir/resolve/diagnostics/ConeDiagnostics.kt) for details.
- `ConeDiagnostic` objects are stored in the FIR tree. The special checker component ([ErrorNodeDiagnosticCollectorComponent](./src/org/jetbrains/kotlin/fir/analysis/collectors/components/ErrorNodeDiagnosticCollectorComponent.kt)) scans all FIR nodes and reports the appropriate diagnostics based on the found `ConeDiagnostic`.

## Platform and Common checkers

In KMP (multiplatform) compilation, the same type may resolve to different classes depending on the use-site session if this type is based on the
 `expect` classifier. This means that the same checker may produce varying results depending on the use-site session:

```kotlin
// MODULE: common
expect interface A

class B : A

// MODULE: platform()()(common)
actual interface A {
    fun foo()
}
```

In this example, `class B` exists in the `common` module, and from the point of view of this module, there are no issues with this class. However, after
actualization, supertype `A` resolves to `actual interface A`, which introduces an `abstract fun foo()` into the scope, making `class B` invalid
because it doesn’t implement this abstract function.

To address this issue, all checkers are divided into two groups: `Common` and `Platform` (see the [MppCheckerKind](compiler/fir/checkers/src/org/jetbrains/kotlin/fir/analysis/checkers/MppCheckerKind.kt) enum):
- `MppCheckerKind.Common` means that the checker should run in the same session to which the corresponding declaration belongs.
- `MppCheckerKind.Platform` means that, in the case of KMP compilation, the checker should run with the session of the leaf platform module for sources of all modules.

The author of each new checker must decide in which session the checker should run and properly set the `MppCheckerKind` in the checker declaration.
The following hints may help with the decision:
- If the checker is not concerned with the scope of a class acquired from a type, scope, or provider, it should be `Common`.
- If the checker requires symbols for a class or type but doesn’t need details about how that class or typealias can be expanded, it is most likely `Common`.
- If the checker is interested in the scope of a type, careful consideration should be given to how the actualization of the scope might affect the checker.

#### Checkers for `expect` classes

```kotlin
// MODULE: common
expect interface A
expect class B : A

class C : A

// MODULE: platform()()(common)
actual interface A {
    fun foo()
}

actual class B : A {
    override fun foo() {}
}
```

In this example, we aim to report "`abstract foo` not implemented" on class `C`, but we don’t want to report this issue on `expect class B` (as its supertype is always `expect A`, never `actual A`).

To handle such cases, it is recommended to split platform checkers into two parts:
- `Regular`: Platform checkers that run for everything except `expect` declarations.
- `ForExpectClass`: Common checkers that run exclusively for `expect` declarations.

As an example, refer to the implementation of the [FirImplementationMismatchChecker](compiler/fir/checkers/src/org/jetbrains/kotlin/fir/analysis/checkers/declaration/FirImplementationMismatchChecker.kt) checker.
