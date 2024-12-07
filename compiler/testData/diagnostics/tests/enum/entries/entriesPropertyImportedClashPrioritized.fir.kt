// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +EnumEntries +PrioritizedEnumEntries
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
    A.entries

    with(A) {
        entries
    }
}
