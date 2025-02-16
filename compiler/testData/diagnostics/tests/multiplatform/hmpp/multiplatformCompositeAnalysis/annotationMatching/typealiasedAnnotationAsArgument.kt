// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -TYPE_MISMATCH
// MODULE: common
expect annotation class Typealiased()

annotation class Ann(val p: Typealiased)

@Ann(Typealiased())
expect fun test()

// MODULE: main()()(common)
annotation class TypealiasedImpl

actual typealias Typealiased = TypealiasedImpl

@Ann(Typealiased())
actual fun test() {}

