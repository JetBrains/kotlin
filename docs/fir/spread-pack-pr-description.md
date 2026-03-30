# Spread Pack Prototype PR Draft

## Summary

This change set prototypes named spread-pack support in the Kotlin compiler and analysis stack.

The current branch covers three layers:

1. Call-site named spread for argument projection, for example `foo(...pack)`.
2. Declaration-side pack expansion for type-based and function-based packs, including selector-based subsets such as `$attrs`, `$callbacks`, and `$props`.
3. Early analysis expansion so function bodies, diagnostics, and scope providers can see pack-expanded parameters as ordinary value parameters.

## User-Facing Syntax

### Call side

- `foo(...pack)`
- `foo(...T.$props(receiver))`
- `foo(...T.$props(receiver).exclude(name1, name2))`

### Declaration side

- `fun f(...T.$props)`
- `fun f(...g.$props)`
- `fun f(...Text.$attrs(materialStringText), ...Text.$callbacks(materialStringText))`

## Semantics

- Spread receivers are evaluated once.
- Argument application preserves source order.
- Explicit named arguments override spread-provided names.
- Earlier spreads win for names that are still unfilled.
- `T.$props(receiver)` binds a single source object and projects the exported property set from that object.
- `.exclude(...)` filters exported names and is checked against the final expanded contract.
- Function packs reuse the resolved public parameter contract of the selected overload, not the function return type.
- Scope providers and body resolution operate on expanded value parameters after pack lowering.

## Real Compose Validation

The branch includes a real Compose Desktop demo under `scratch/pack-compose-demo` instead of a parser-only sample.

Validated scenarios:

- `...Text.$props(materialStringText)` in `AddText`
- `...AddText.$props` in `TitleText`
- selector split via `...Text.$attrs(materialStringText)` and `...Text.$callbacks(materialStringText)`
- bound-source projection via `...TextPropsCarrier.$props(this).exclude(fontSize, fontWeight)` in `BoundTitleText.Render`

Local validation results:

- `./gradlew --stop`
- `./gradlew --dependency-verification=off :kotlin-compiler-embeddable:publishToMavenLocal :plugins:compose-compiler-plugin:compiler:publishToMavenLocal :compiler:build-tools:kotlin-build-tools-impl:publishToMavenLocal`
- `./gradlew --dependency-verification=off :kotlin-stdlib-common:publishToMavenLocal :kotlin-stdlib:publishToMavenLocal`
- `./gradlew --dependency-verification=off --no-configuration-cache -p scratch/pack-compose-demo clean compileKotlin`
- `./gradlew --dependency-verification=off --no-configuration-cache -p scratch/pack-compose-demo run`
- `./gradlew --dependency-verification=off --no-configuration-cache -p scratch/pack-compose-demo verifyVisualDemo`

Observed runtime state:

- Compose demo process launched successfully as `demo.MainKt`
- Runtime JDK was `JDK 21.0.8`
- Visual verification screenshot was generated at `scratch/pack-compose-demo/build/reports/visual-verification/pack-compose-demo.png`
- compiler distribution zip was produced by `./gradlew --dependency-verification=off zipCompiler`

## Packaging

Generated compiler artifact:

- `dist/kotlin-compiler-2.4.255-SNAPSHOT.zip`

## Reviewer Notes

- This branch intentionally includes compiler, analysis API, parser, PSI, diagnostics, and demo validation work together because declaration packs are not useful if bodies and scopes still treat them as opaque syntax.
- Stock IntelliJ Kotlin plugins will not understand the new syntax until matching analysis and IDE artifacts from the same fork are used.
- PR publication is still blocked on GitHub setup for a real fork remote under `zjarlin`.
