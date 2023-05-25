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
    A.<!DEBUG_INFO_CALL("fqName: foo.A.Companion.entries; typeCall: variable"), DEPRECATED_ACCESS_TO_ENUM_ENTRY_COMPANION_PROPERTY!>entries<!>

    with(A) {
        <!DEBUG_INFO_CALL("fqName: foo.A.Companion.entries; typeCall: variable")!>entries<!>
    }
}
