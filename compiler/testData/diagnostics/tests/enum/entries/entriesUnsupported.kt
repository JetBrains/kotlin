// !LANGUAGE: -EnumEntries
// WITH_STDLIB
// ISSUE: KT-55251
// KT-67020: K2: Turning off `EnumEntries` crashes IR Interpreter
// SKIP_FIR2IR

enum class Foo {
    BAR;
}

fun main() {
    Foo.<!UNSUPPORTED_FEATURE!>entries<!>
}
