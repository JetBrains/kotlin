// !LANGUAGE: -EnumEntries

package foo

import foo.A.Companion.entries

enum class A {
    ;

    companion object {
        val entries = 0
    }
}

fun test() {
    A.entries

    with(A) {
        entries
    }
}
