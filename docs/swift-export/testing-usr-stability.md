# Swift Export USR Stability

Swift symbol USRs (Unified Symbol Resolution) are compiler-generated identifiers emitted by
`swift symbolgraph-extract`. The Swift export tests in this repository hardcode USR strings to
assert a stable public API surface for Swift consumers.

## What the tests assume

The tests assume that, for the same Kotlin sources and export configuration, the resulting USR
strings remain stable across runs. This is generally true within a fixed toolchain, but USRs can
change when:

- The Swift compiler version changes its mangling scheme
- Kotlin/Native Swift export changes how it maps or mangles symbols
- The exported API surface changes (new/removed/renamed declarations)

A change in USRs is not automatically a regression. It can be either:

- An intentional API surface change
- A toolchain change that requires updating expected USRs

## Updating expected USRs

1. Run the relevant Swift export integration tests.
2. Inspect the assertion failure output to find the actual USR values.
3. Update the expected `SwiftSymbol` entries in the test.
4. Re-run tests to confirm the API surface is as expected.

## Where to look

- `libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/native/SwiftExportIT.kt`
- `libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/kotlin/org/jetbrains/kotlin/gradle/util/SwiftExportUtils.kt`
