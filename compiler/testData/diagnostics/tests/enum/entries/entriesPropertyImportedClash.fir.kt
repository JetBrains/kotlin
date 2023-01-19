// !LANGUAGE: -EnumEntries
// WITH_STDLIB

package foo

import foo.A.Companion.entries

enum class A {
    ;

    companion object {
        val entries = 0
    }
}

fun test() {
    A.<!OPT_IN_USAGE_ERROR!>entries<!>

    with(A) {
        entries
    }
}
