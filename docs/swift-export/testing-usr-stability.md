# Swift Export USR Stability

Swift symbol USRs (Unified Symbol Resolution) are compiler-generated identifiers emitted by
`swift symbolgraph-extract`. The Swift export integration tests assert on demangled USRs
(`SwiftSymbol.demangledId`) for readability, while the raw USR is used only during extraction.

## How it works

1. `extractModuleSymbols` reads the symbolgraph JSON and extracts raw USRs and `pathComponents`.
2. Each raw USR is demangled via `xcrun swift-demangle --compact` and stored in
   `SwiftSymbol.demangledId`.
3. Equality and hashing are based on demangled ID only.
4. `toString()` returns `demangledId`, so assertion failure diffs are human-readable.

Compound USRs containing `::SYNTHESIZED::` (used for protocol extension members that Swift
synthesizes automatically, such as `localizedDescription` from `Foundation.Error`) are split,
each part demangled separately, and reassembled as
`"<demangled lhs> [SYNTHESIZED for <demangled rhs>]"`.

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

## Updating expected symbols

1. Run the relevant Swift export integration tests.
2. Inspect the assertion failure output to find the actual values.
3. Update the expected `SwiftSymbol` entries in the test (`demangledId` and
   `pathComponents`).
4. To demangle a raw USR manually, use `--compact` (matches the tests) and either:
    ```
    xcrun swift-demangle --compact '$s6Shared3fooyyF'
    ```
   or (stdin form requires the `$s` prefix explicitly):
    ```
    echo '$s6Shared3fooyyF' | xcrun swift-demangle --compact
    ```
5. Re-run tests to confirm the API surface is as expected.

## Where to look

- `SwiftExportIT.kt` — test expectations with hardcoded symbols
- `SwiftExportUtils.kt` — `SwiftSymbol`, parsing, demangling, and assertion helpers
