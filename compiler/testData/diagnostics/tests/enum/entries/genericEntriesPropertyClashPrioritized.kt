// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +EnumEntries +PrioritizedEnumEntries
// WITH_STDLIB
// FIR_DUMP

package pckg

enum class A {
    ;

    companion object
}

val <T> T.entries: Int get() = 0

fun test() {
    val i: Int = A.<!DEBUG_INFO_CALL("fqName: pckg.entries; typeCall: variable"), DEPRECATED_ACCESS_TO_ENUM_ENTRY_COMPANION_PROPERTY!>entries<!>
    A.Companion.<!DEBUG_INFO_CALL("fqName: pckg.entries; typeCall: variable")!>entries<!>
}
