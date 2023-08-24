// !LANGUAGE: +EnumEntries -PrioritizedEnumEntries
// WITH_STDLIB
// FIR_DUMP

package foo

import foo.A.Companion.entries

enum class A {
    ;

    companion object {
        val entries = 0
    }
}

fun test() {
    <!DEPRECATED_ACCESS_TO_ENUM_ENTRY_COMPANION_PROPERTY!>A.entries<!>

    with(A) {
        entries
    }
}
