// FIR_IDENTICAL
// DIAGNOSTICS: -TYPE_MISMATCH
// MODULE: common
// TARGET_PLATFORM: Common
expect annotation class Typealiased()

annotation class Ann(val p: Typealiased)

@Ann(Typealiased())
expect fun test()

// MODULE: main()()(common)
annotation class TypealiasedImpl

actual typealias Typealiased = TypealiasedImpl

@Ann(Typealiased())
actual fun test() {}

