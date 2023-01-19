// !LANGUAGE: -EnumEntries
// WITH_STDLIB
// ISSUE: KT-55251

enum class Foo {
    BAR;
}

fun main() {
    Foo.<!UNRESOLVED_REFERENCE!>entries<!>
}
