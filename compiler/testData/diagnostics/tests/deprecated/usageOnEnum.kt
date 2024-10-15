// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +NoDeprecationOnDeprecatedEnumEntries
// ISSUE: KT-37975

@Deprecated("")
enum class Foo(val x: Int) {
    A(42)
}
