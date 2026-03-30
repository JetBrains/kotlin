# Spread Packs Design Note (Pre-KEEP)

Status: pre-KEEP draft for the prototype branch `codex/spread-operator-prototype`

Related prototype:

- PR: https://github.com/JetBrains/kotlin/pull/5799
- Prototype summary: [spread-pack-pr-description.md](spread-pack-pr-description.md)
- Test matrix: [spread-pack-test-matrix.md](spread-pack-test-matrix.md)

This note is intentionally not presented as an official KEEP. The verified Kotlin contribution flow for new language features is: first open a Language Design discussion and YouTrack issue, then move to Kotlin/KEEP after language-design approval.

## Problem

Kotlin wrappers around large named-parameter APIs are expensive to maintain.

Typical examples include:

- Compose wrappers around `Text`, `Button`, and similar UI functions
- façade APIs that forward most named parameters with a few overrides
- component adapters that want to separate "plain attributes" from callbacks and slots
- wrappers that forward fields from a carrier object such as `T.$props(this)`

Today, Kotlin has no language-level way to:

- forward a named parameter pack at the call site
- import another callable's public parameter contract into a declaration
- project a subset of exported parameters such as non-function attributes or callbacks
- bind a property pack to a carrier object and exclude a few names

The result is repeated boilerplate, repeated wrapper drift, and body scopes that do not automatically stay in sync with the forwarded contract.

## Goals

- Allow named parameter forwarding at call sites without reflection or maps.
- Allow declarations to import parameter contracts from types, function overloads, and function-typed values.
- Preserve ordinary parameter behavior inside function bodies after lowering.
- Support Compose-style API splitting:
  - `$attrs` for non-function parameters
  - `$callbacks` for non-composable function parameters
  - `$slots` for composable function parameters
- Keep evaluation order explicit and deterministic.
- Keep overload handling explicit enough to avoid unstable or surprising APIs.

## Non-goals

- Runtime reflection-based argument bags
- Structural anonymous record types
- Implicit merging of incompatible overload sets
- Preserving the source pack syntax in binary metadata as ABI
- Shipping the syntax in stock IntelliJ without matching analysis and IDE changes

## Proposed syntax

### Call-site spread

```kotlin
foo(...pack)
foo(...T.$props(receiver))
foo(...T.$props(receiver).exclude(name1, name2))
```

### Declaration-side packs

```kotlin
fun f(...T.$props)
fun f(...g.$props)
fun f(...Text.$props(textString))
fun f(...Text.$sharedProps)
fun f(...Text.$attrs(textString), ...Text.$callbacks(textString))
```

### Function value aliases

```kotlin
val textString: (text: String, color: String) -> Unit = ::Text

fun Wrapper(...textString.$props)
fun Wrapper(...Text.$props(textString))
```

## Selector kinds

The current prototype recognizes five selectors.

- `$props`
  Includes the full exported parameter set.
- `$sharedProps`
  Only valid for function overload sets. Includes the intersection of parameter names that exist on every overload with the same type.
- `$attrs`
  Non-function parameters.
- `$callbacks`
  Function parameters that are not composable slots.
- `$slots`
  Composable function parameters.

For Compose-style wrappers, the split is intentional:

- `$attrs` maps to ordinary value-like inputs
- `$callbacks` maps to event/lambda callbacks
- `$slots` maps to composable content lambdas

This is more useful than a single `$props` bucket when wrappers need to forward callbacks and slots separately.

## Type-pack semantics

`T.$props` expands visible readable non-extension instance properties of `T`.

Recommended semantic rules:

- Walk supertypes before the current class, so ordering is base-to-derived.
- Preserve declaration order within each type.
- Collapse overrides to the most specific declaration.
- Type packs may provide defaults when the selected property has a direct initializer or a primary-constructor default that can be lowered safely.
- If expansion produces duplicate final parameter names, report a hard diagnostic.

Example:

```kotlin
open class LabelProps(
    val text: String,
    val enabled: Boolean = true,
)

fun Label(...LabelProps.$props) {
    println("$text $enabled")
}
```

## Function-pack semantics

Function packs import the final public value-parameter contract of a callable.

Important rule:

- `g.$props` means "reuse `g`'s public value parameters", not "`g`'s return type properties".

### Single callable or function-typed value

If the receiver resolves to exactly one callable, or to a function-typed value, the selected parameter contract is imported directly.

### Overload sets

Bare overload sets must not silently guess.

Recommended final design:

- `Text.$props` on an overload set is an error if there is no unique selected callable.
- `Text.$sharedProps` is the explicit "intersection across overloads" form.
- `Text.$props(textString)` is the explicit "pick the overload matching this function-typed value" form.

This is the key overload decision for upstream review.

#### Why not generate `Text.$props1`, `Text.$props2`?

This design was considered and rejected.

Problems:

- numbering is unstable under source reordering and refactoring
- numbering leaks implementation ordering into API surface
- it is poor across modules because declaration order is not a good semantic contract
- it scales badly once overload sets evolve

#### Why not merge all overload parameters into one giant pack?

This was also rejected.

Problems:

- the merged surface hides which overload is actually intended
- requiredness and defaults become ambiguous
- equal names with different types become either unsound or arbitrary
- wrapper APIs become harder to read and refactor

The explicit pair of `$sharedProps` and `$props(selector)` is a better contract.

## Bound source semantics

`T.$props(receiver)` means:

1. evaluate `receiver` exactly once
2. project the selected exported members from that evaluated object
3. map projected names onto the callee's parameters

Example:

```kotlin
Text(...TextPropsCarrier.$props(this).exclude(fontSize, fontWeight))
```

Recommended rules:

- `this` is valid only where a dispatch or extension receiver exists
- the bound source must be type-compatible with `T`
- `.exclude(...)` filters by final exported names after pack expansion
- unknown excluded names should be diagnosed explicitly

## Call-site evaluation rules

The prototype and proposal share the same intended call semantics.

- Spread receivers are evaluated once.
- Arguments are applied in source order.
- Explicit named arguments override spread-provided names.
- Earlier spreads win over later spreads for still-unfilled names.
- Missing required parameters are still diagnosed normally.

## Declaration lowering model

Declaration packs should lower to ordinary value parameters early enough that:

- function bodies resolve against ordinary parameters
- scope providers expose those parameters directly
- diagnostics run on the expanded parameter contract
- backends do not need pack-aware calling conventions

This is the main reason the prototype touches parser, PSI, FIR, K1 resolution, IR generation, decompiler/stubs, and analysis scope providers together.

## Tooling implications

Compiler-only changes are not enough.

- Stock IntelliJ Kotlin plugins will reject the syntax until matching parser and analysis changes are shipped from the same fork.
- Body completion for `fun f(...T.$props)` only works if analysis/IDE modules also see the expanded parameter scope.
- Binary dependencies should expose the expanded public contract, not require source-only pack syntax to be understood downstream.

## Diagnostics

The proposal needs dedicated diagnostics for at least:

- unresolved pack receiver
- invalid selector for the receiver kind
- ambiguous function pack selection
- no overload matching the explicit selector
- empty `$sharedProps`
- duplicate exported parameter names after expansion
- recursive function-pack cycles
- invalid `this`
- incompatible bound source type
- unknown names in `.exclude(...)`

## Alternatives considered

### Plain call-site `foo(...pack)` only

Insufficient. It helps forwarding but does not solve declaration boilerplate or body completion.

### Type packs only

Insufficient. A large Compose-style surface is often defined by callable signatures rather than carrier classes.

### Function packs only

Insufficient. Bound carrier objects like `T.$props(this)` are a useful and simpler source for wrapper state.

### Only `$props`, no `$attrs` / `$callbacks` / `$slots`

This misses a major UI use case. Real wrappers often need callbacks and slots separated from ordinary attributes.

## Prototype status

The prototype branch already demonstrates:

- call-site spread mapping
- declaration-side pack expansion
- type packs
- function packs
- function-value overload selectors such as `Text.$props(textString)`
- overload intersections via `$sharedProps`
- Compose-oriented selectors: `$attrs`, `$callbacks`, `$slots`
- bound carrier projection via `T.$props(this)`
- `.exclude(...)` support at call sites
- analysis scope exposure for expanded parameters
- cross-module pack import tests
- a real Compose Desktop smoke demo and visual verification task

Known gaps before upstream merge should even be discussed seriously:

- the feature is not yet behind an experimental language flag
- IDE shipping is still separate from this repository
- diagnostics and K1/K2 parity still need more hardening
- `.exclude(...)` needs explicit unknown-name diagnostics in the final design
- the formal proposal should live in Kotlin/KEEP after language-design approval, not only in this repository

## Recommended upstream path

1. Open a YouTrack Language Design issue with this design note and the prototype PR attached.
2. Get agreement on the overload model:
   - bare overload sets are errors
   - `$sharedProps` is the explicit intersection form
   - `$props(selector)` is the explicit concrete-overload form
3. Decide whether the first accepted slice should include:
   - only `...pack`
   - `...pack` plus declaration packs
   - the full selector family
4. Add an experimental language feature gate before asking for serious implementation review.
5. Move the language-level proposal into Kotlin/KEEP after design approval.
