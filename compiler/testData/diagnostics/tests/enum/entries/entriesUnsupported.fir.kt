// !LANGUAGE: -EnumEntries
// WITH_STDLIB
// ISSUE: KT-55251

enum class Foo {
    BAR;
}

fun main() {
    Foo.<!OPT_IN_USAGE_ERROR!>entries<!>
}
