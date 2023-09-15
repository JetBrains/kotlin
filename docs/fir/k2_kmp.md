# K2 KMP (Kotlin multiplatform) implementation documentation

This document describes the implementation of the KMP support in the K2 Compiler along components.

**Note**: The current compilation model is temporary, and should be changed to final in [KT-57327](https://youtrack.jetbrains.com/issue/KT-57327)

## Document structure

- [Glossary](#glossary)
- [Metadata Compilation](#metadata-compilation)
- [Platform Compilation](#platform-compilation)
  - [FIR2IR](#fir2ir)
  - [IRActualizer](#iractualizer)
  - [Fake overrides](#fake-overrides)
- [Frontend support for expect/actual](#frontend-support-for-expectactual)
  - [Type refinement](#type-refinement)
  - [Default propagation](#default-propagation)
  - [Actual -> expect binding construction](#fir-actual---expect-binding-construction)
- [Frontend checkers](#frontend-checkers)

## Glossary

### Source-set

A bunch of source files along with its depends-on relations with other source-sets.

See: https://kotlinlang.org/docs/multiplatform-discover-project.html#source-sets

**Note:** Binary dependencies are provided on the [compilation](#compilation) level and not considered a part of the source-set definition 
by the compiler.

### Module

A compiler module, entity with source-code and dependencies. 
  - One-to-one mapped to the corresponding FirSession.
  - One-to-one mapped to the FirModuleData.
  - Compiler representation of a [source-set](#source-set)

In a [platform compilation](#platform-compilation), 
[multiple modules are created from the input](#source-set-and-module-relation) [source-sets](#source-set).   

### Compilation

A single invocation of the compiler entry-point

### Metadata dependencies/KLibs

- KLib that only contains frontend metadata, without IR. It can only be used to analyze dependent code.

### HMPP

Hierarchical multi-platform projects
- See https://kotlinlang.org/docs/multiplatform-hierarchy.html

### Actualization

The process of replacing references to expect declarations with corresponding actual declarations. 
See [IRActualizer](#iractualizer)

### actual->expect binding

A relation between two declarations that form an expect-actual pair.
- For classes: defined as identical ClassId. 
- For callables: defined as complex matching rule, which is similar to overloading.

In general case, the relation is many-to-many. 
However, in correct code it is either one-to-one, or one-to-many in case of type-aliases.

See [FIR: actual -> expect binding construction](#fir-actual---expect-binding-construction)

### Common source-set

A [source-set] that may contain expect declarations. 
As opposed to a platform [source-sets](#source-set) that must only contain actual or regular declarations. 

Common source-sets usually contain code that is shared across different targets, and by that is included 
in multiple [platform compilations](#platform-compilation).

However, one may define [HMPP](#hmpp) hierarchy with only one target while still having common source-sets. 

## Metadata Compilation

- Aimed to check code platform-independent compilation.
- Each [common source set](#common-source-set) is compiled with its metadata binary dependencies.
- Artifacts aren't used for subsequent platform compilations but for the IDE and other metadata compilations.
- In this compilation, we analyze code as if it is fully isolated from the corresponding actualizations.
  - Doing so will allow us to check that code can be compiled for multiple targets.
  - Only the [common source-set](#common-source-set) and its [metadata binary dependencies](#metadata-dependenciesklibs) are taken into account.
  - No access to the target-specific dependencies or source-sets.
  - Source-sets from depends-on are provided as [metadata binary dependencies](#metadata-dependenciesklibs) in the form of unpacked klib
    - See `-Xrefines-path`

**Entry-point:** [`org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler`](../../compiler/cli/src/org/jetbrains/kotlin/cli/metadata/K2MetadataCompiler.kt)

Also, see: [`org.jetbrains.kotlin.cli.metadata.FirMetadataSerializer`](../../compiler/cli/src/org/jetbrains/kotlin/cli/metadata/FirMetadataSerializer.kt)

**Inputs:**
- [Metadata KLibs for dependencies](#metadata-dependenciesklibs)
- [Metadata KLibs for dependsOn source-sets](#metadata-dependenciesklibs)
- Source files of a single [common source-set](#common-source-set), such as "commonMain"

**Outputs:**
- [Metadata KLibs](#metadata-dependenciesklibs)

In this mode, we analyze only common sources.
This compilation mode is very similar to a usual compilation pipeline with the exception that no IR is serialized to the output.  

Actual-expect [matching](#frontend-support-for-expectactual) and [checking](#frontend-checkers) are performed,
since in [HMPP](#hmpp) we can have actuals in metadata compilation inputs.


**Note:** The full set of actual declarations can't be determined at that point. 
The only actual -> expect binding could be obtained and checked.

**Note:** In this compilation mode [actual->expect binding](#actual-expect-binding) may point to a deserialized element,
since expect declarations from depends-on source-sets will be present as [metadata binary dependencies](#metadata-dependenciesklibs).

## Platform Compilation

- Invoked per each target.
- Input includes [source-sets](#source-set) from entire target depends-on graph.
- Outputs a platform artifact (jar file or klib).
- [Source-sets](#source-set) form a directed acyclic graph.
  - The graph must be terminated by one leaf [source-set](#source-set)
- Compilation requires a platform classpath. See: [binary dependencies](#platform-compilation-binary-dependencies)

Please consult with https://kotlinlang.org/docs/multiplatform-hierarchy.html for more info on source-set structure. 

This is the main part where the pipeline diverges from simple compilation.

Ex:
```kotlin
// MODULE: common
expect fun a1(): String
expect fun a2(): String

// MODULE: intermediate()()(common)
actual fun a1(): String = ""
expect fun a4(): String
expect fun a5(): String

// MODULE: unrelated
expect fun a3(): String
expect fun a5(): String

// MODULE: platform()()(intermediate, unrelated)
actual fun a2(): String = ""
actual fun a3(): String = ""
actual fun a4(): String = ""
actual fun a5(): String = "" // ambiguous
```

In the given example, modules aka source-sets form the following dependency graph:
```
common
^
|
intermediate   unrelated
^            ^
|          /
|        /
platform
```

### Source-set and module relation

During the platform compilation, the entire [source-set](#source-set) graph is passed to the compiler CLI.

See: `-Xfragments`, `-Xfragment-sources`, `-Xfragment-refines`.

When combined with [binary dependencies](#platform-compilation-binary-dependencies) passed, it allows constructing graph of 
compiler [modules](#module).

Compiler [module](#module) is created for each passed [source-set](#source-set).

### Platform compilation binary dependencies

The K2 uses shared **platform binary dependencies** for analysis of **all** source-sets in the platform compilation.

The key reason for that is that we are unable to provide full KLibs for [common source-sets](#common-source-set) yet.

Observed behavior is the following:
```kotlin
// MODULE: common_dep
// library: dep
fun foo(a: Any) {} // (dep.1)

// MODULE: platform_dep
// library: dep
fun foo(a: String) {} // (dep.2)

// MODULE: common
// dependencies { implementation("dep") }
fun bar(a: Any) {} // (a.1)

fun test() {
    bar("") // (a.1)
    foo("") // (dep.2) !!! While platform compilation for this module 
}

// MODULE: platform
// depends-on: common
fun bar(a: String) {} // (a.2)
```

### Platform compilation pipeline

Given that, source analysis order is defined as the following:

Analyze from the most common module to the platform modules.
So that all `depends-on` modules of the module are analyzed before the module itself.

In order to achieve it, [modules](#module) are sorted topologically over depends-on relation graph. 

Platform compilation pipeline consists of the following steps:
1. For each [module](#module) in source analysis order
   - [Use a shared set of binary dependencies](#platform-compilation-binary-dependencies) 
   - [Run the frontend, store FIR representation.](#frontend-support-for-expectactual) 
     - Use FIR representations of depends on modules that were obtained before as dependencies.
     - *[Resolution requires knowledge of actual->expect binding.](#fir-actual---expect-binding-construction)
   - [Run the frontend checkers for module FIR.](#frontend-checkers)
   - [Run the FIR2IR, store the resulting IR module fragment.](#fir2ir)
     - *[Re-use the FIR2IR state](#fir2ir-shared-state) 
2. [Combine all resulting IR module fragments](#iractualizer)
   - Perform [actualization](#actualization)
   - Check that every expect declaration has corresponding actual and was actualized.
   - *Reconstruct fake-overrides.
   - Remove expect declarations.
   - Merge IR module fragments into last fragment
     - Now, we have complete IR for module that doesn't reference any expects and could be passed to backend.  
3. Pass the resulting IR to the backend or serialize it into KLib.
   - Backend compiler plugins are invoked before running the backend

### FIR2IR

FIR2IR is applied to the modules in the topological order.

**Inputs:**
- FIR of one of the [modules](#module)
- [Compilation shared state](#fir2ir-shared-state)

**Outputs:**
- IR for the module
- Mutation of [shared state](#fir2ir-shared-state)

#### FIR2IR shared state

The FIR2IR state, represented by `Fir2IrCommonMemberStorage` is shared across entire [platform compilation](#platform-compilation).

See: [`org.jetbrains.kotlin.fir.backend.Fir2IrCommonMemberStorage`](../../compiler/fir/fir2ir/src/org/jetbrains/kotlin/fir/backend/Fir2IrCommonMemberStorage.kt)

Since FIR2IR is invoked over each [module](#module) over depends-on graph,
we need to avoid creating IR for declarations in a [common source-sets](#common-source-set) multiple times. 

The same applies to the declarations from [binary dependencies](#platform-compilation-binary-dependencies), which are shared among the entire compilation.

Therefore, FIR2IR uses shared storage for get-or-create operations for declarations.

### IRActualizer

The IR actualizer is a component performing [actualization](#actualization) over IR 

See: [`org.jetbrains.kotlin.backend.common.actualizer.IrActualizer`](../../compiler/ir/backend.common/src/org/jetbrains/kotlin/backend/common/actualizer/IrActualizer.kt)

In the current model, IR actualizer is used during the [platform compilation](#platform-compilation), to produce complete IR that is correct
from the backend standpoint.

**Inputs:**
- IR for each of the modules.
- IR might contain expect declarations and references to it. 

**Outputs:**
- Single IR module.
- No expect declarations in the IR. 
- IR is in the usual state, as for non-multiplatform projects

**Constraints:**
- No access to FIR or frontend state allowed
- Must operate over IR
- Diagnostic reporting is complicated and won't work for the IDE without special support

The following actions are preformed:
- Expect -> actual binding is constructed
  - We have all expects and that's why we can construct it as opposed to the [actual -> expect binding](#actual-expect-binding)
  - We can report diagnostics at that moment
- Top-level expect declarations are detached from IrModuleFragment
  - Member expect declarations can only be contained within top-level expects, and is detached together with its container
  - Modulo [`@OptionalExpectation`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-optional-expectation/) 
- IR for default values is copied from expects to actuals. See [default propagation](#default-propagation) for the frontend part of work.
- All module fragments are merged into one
- [Fake-override actualization is performed](#fake-overrides)

### Fake overrides

FIR doesn't use fake-overrides in the frontend, and that's why we construct them during FIR2IR in order to match backend expectations
By that, we require special handling of fake-overrides during the [platform compilation](#platform-compilation).

during the analysis and couldn't construct fake-overrides properly. 

Ex: 
```kotlin
// MODULE: common
expect class A
expect class B


interface I {
    fun foo(q: A)
}

interface J {
    fun foo(q: B)
}

interface K : I, J {
    // FIR2IR F/O fun foo(q: A) (1)
    // FIR2IR F/O fun foo(q: B) (2)
}

// MODULE: platform()()(common)

actual typealias A = Int
actual typealias B = Int

class Impl : K {
    override fun foo(q: Int) {}
}

```

In the given example during the actualization, we need to combine (1) and (2) into one fake-override.

## Frontend support for expect/actual

During the resolution of each module, a special set of measures is implemented to allow proper resolution.

### Type refinement

Type refinement is a process of obtaining use-site view to the declaration.
Since we re-use FIR of each module during the analysis of its dependants, we need to perform 
type refinement.

```kotlin
// MODULE: common
expect class Foo 

val foo: Foo = TODO() 
// declaration-site type: expect class Foo

// MODULE: platform()()(common)
actual class Foo {
    fun bar() { }
}

fun test() {
    foo.bar() // use-site type: actual class Foo
}
```

**It is possible due to the following principles:**
- It isn't possible to obtain the scope of a type without knowing the use-site
- It isn't possible to obtain the type declaring class without knowing the use-site
- Every type is subject to the refinement before use
- ClassId can only be used to identify class within one module

Refinement of a type happens in two stages:
1. `ConeKotlinType.fullyExpandedType(useSiteSession: FirSession): ConeKotlinType`
   - At that stage, we unwrap any type-aliases in the type using useSiteSession as a point of view.
2. `ConeClassifierLookupTag.toSymbol(useSiteSession: FirSession): FirClassifierSymbol<*>`
   - All classifier types contain lookupTag inside them. 
   - To obtain reference to the type declaring class, one should call toSymbol on it providing the useSiteSession as a point of view.
   - Then, the lookup of the class in the use-site module will be performed based on the class id stored within the lookup tag.

**WARNING:** Type refinement is also needed for general dependency substitution algorithm, such as classpath order substitution.

**It works for expect/actual classes only because [actual->expect binding](#actual-expect-binding) is defined as a matching of ClassId** 

### Default propagation

We need to know a set of arguments that should be provided for a call to resolve it due to the Kotlin resolution and overloading rules. 

```kotlin
// MODULE: common
expect fun foo(a: String = "")
expect class Bar {
    fun buz(a: String = "") 
} 
// MODULE: intermediate()()(common)
actual fun foo(a: String) {}
actual class Bar {
    fun buz(a: String) {}
}

fun test() {
    foo() // use-site
    Bar().buz() // use-site
}
```

In the given example, we need to know [actual->expect binding](#actual-expect-binding) in order to resolve use-site calls.

As during the resolution on the use-site, we will resolve to the actual declarations, we need to obtain its default argument positions from
actuals. 

### FIR: actual -> expect binding construction

We compute that binding by analyzing module with `FirExpectActualMatcherTransformer`.

See: [`org.jetbrains.kotlin.fir.resolve.transformers.mpp.FirExpectActualMatcherTransformer`](../../compiler/fir/resolve/src/org/jetbrains/kotlin/fir/resolve/transformers/mpp/FirExpectActualMatcherTransformer.kt)

**The responsibility** of this phase is to perform [expect/actual matching](#matching-vs-checking).

This phase happens before body/implicit type resolution
as the binding is required to perform [call resolution in case of defaults](#default-propagation)

Binding is stored in the `FirDeclaration.expectForActual` attribute and allow to determine expects that corresponds to the
actual declaration during the analysis of `intermediate` module.

**Hard-constraint:**
`FirExpectActualMatcherTransformer` cannot use return types of declarations to bind actual with expect.

Since the return type of actual function might be not yet resolved before the implicit type body resolve phase.

```kotlin
// MODULE: common
expect fun foo(a: String = "")

// MODULE: platform()()(common)
actual fun foo(a: String) = run {
    foo() // In order to resolve this call, we need to have actual -> expect binding
}
```

**Explanation:** While it is possible to compute the binding on-demand and remove this constraint,
we chose to have it as a separate phase to avoid further complication of body resolve.

There are no overloads by return type in Kotlin, and it makes it possible to avoid return type matching as a part of
[actual->expect binding construction](#fir-actual---expect-binding-construction).
However, we still [check](#frontend-checkers) that return type matches afterward.  

#### Matching vs checking

Currently, both matching and a part of checking are performed in the `AbstractExpectActualCompatibilityChecker`.

See: [`org.jetbrains.kotlin.resolve.calls.mpp.AbstractExpectActualCompatibilityChecker`](../../compiler/resolution.common/src/org/jetbrains/kotlin/resolve/calls/mpp/AbstractExpectActualCompatibilityChecker.kt)

Matching is a process of finding expect-actual pairs. 
For classes, it is matching of class-ids.
For callables, it is a complex rule, similar to overloading.
If declarations don't match, no pair is formed and **matching continues** over other declarations.

Checking is a process of checking that a pair is correct w.r.t all compatibility requirements.
If checking failed for a pair, error is reported for the pair. 
It will fail compilation. 

## Frontend checkers

Frontend checkers are executed in the context of each module (use-site), after its resolution.

**Inputs:**
- FIR of one of the [modules](#module)
  - It can contain both expects and actuals

**Constraints:**
- The full set of actuals aren't known yet
- [Actual -> expect binding](#actual-expect-binding) is available
- [Limitations](#limitations)

### Limitations

The key limitation is the fact that frontend checkers are run in the context of declaration site. 

Thus, it is impossible to observe member scopes of classes with respect to actualization since the corresponding actuals
are contained in further modules.

Type checks can also be performed with knowledge available on expect declaration site.

In order to reduce the need to perform additional checks on the backend and in the [IRActualizer](#iractualizer) it is advised to
follow the [LSP](https://en.wikipedia.org/wiki/Liskov_substitution_principle) in the design.

By that, meaning that actual declaration must be as compatible with the corresponding expect declaration, as that
it could replace the expect declaration, without the appearance of errors.

**Note:** There are compiler checks that violate LSP to the extent that it is reported on certain leaf types.
It is expected that such checks won't trigger in case of actualization. 

E.g: 
```kotlin
// MODULE: common
expect class E()

fun foo() {
    E() // It's OK, since it isn't deprecated in common
}

// MODULE: platform()()(common)
@Deprecated("Not OK")
actual class E actual constructor() {} 
```
