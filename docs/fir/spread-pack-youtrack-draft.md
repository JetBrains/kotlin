# Draft YouTrack Language Design Ticket

Suggested project: `KT`

Suggested subsystem: `Language Design`

Suggested title:

`Language Design: spread packs for named argument forwarding and declaration-side parameter import`

Suggested description:

```md
## Summary

I prototyped a Kotlin language feature for "spread packs": a way to forward named argument sets at call sites and import parameter contracts into declarations.

Prototype PR:
- https://github.com/JetBrains/kotlin/pull/5799

Design note:
- `docs/fir/spread-pack-design-note.md`

Validation summary:
- `docs/fir/spread-pack-test-matrix.md`

## Problem

Kotlin wrappers around large named-parameter APIs are repetitive and drift-prone. This is especially painful for Compose wrappers and other forwarding APIs where a wrapper wants to:

- forward most named parameters
- override only a few values
- keep body completion synchronized with the forwarded contract
- separate ordinary attributes from callbacks and slots
- forward from a carrier object such as `T.$props(this)`

## Prototype syntax

### Call side

```kotlin
foo(...pack)
foo(...T.$props(receiver))
foo(...T.$props(receiver).exclude(name1, name2))
```

### Declaration side

```kotlin
fun f(...T.$props)
fun f(...g.$props)
fun f(...Text.$props(textString))
fun f(...Text.$sharedProps)
fun f(...Text.$attrs(textString), ...Text.$callbacks(textString))
```

## Current prototype semantics

- spread receivers are evaluated once
- source order is preserved
- explicit named arguments override spread-provided names
- earlier spreads win for still-unfilled names
- declaration packs are lowered to ordinary value parameters for body resolution and analysis
- selector kinds currently include:
  - `$props`
  - `$sharedProps`
  - `$attrs`
  - `$callbacks`
  - `$slots`

## Main design question

How should function overload sets behave?

My recommended direction is:

- bare `Text.$props` on an overload set is an error
- `Text.$sharedProps` explicitly means "intersection shared by all overloads"
- `Text.$props(textString)` explicitly chooses the overload matching a function-typed value

I do **not** recommend generated ordinal aliases like `Text.$props1`, `Text.$props2`, because they are unstable under source reordering and are a weak cross-module contract.

I also do **not** recommend implicitly merging all overload parameters into one giant pack, because that makes requiredness, defaults, and type conflicts ambiguous.

## Why this may be worth discussing

The prototype already shows practical value on real Compose-style wrappers:

- `...Text.$props(materialStringText)`
- `...AddText.$props`
- `...Text.$attrs(materialStringText)`
- `...Text.$callbacks(materialStringText)`
- `...TextPropsCarrier.$props(this).exclude(fontSize, fontWeight)`

This reduces wrapper boilerplate while keeping the wrapper body typed against ordinary expanded parameters.

## Request

I am not asking for merge readiness yet. I want language-design feedback on:

1. whether this feature direction is acceptable at all
2. whether overload handling should use the `$sharedProps` plus `$props(selector)` model
3. whether Compose-oriented selector families such as `$attrs` / `$callbacks` / `$slots` are in scope for an initial design
4. whether the first acceptable slice should be smaller than the current prototype

If the direction is acceptable, I can convert the design note into a formal KEEP and gate the implementation behind an experimental language feature.
```

Suggested attachments and links:

- `docs/fir/spread-pack-design-note.md`
- `docs/fir/spread-pack-pr-description.md`
- `docs/fir/spread-pack-test-matrix.md`
- `scratch/pack-compose-demo/build/reports/visual-verification/pack-compose-demo.png`
