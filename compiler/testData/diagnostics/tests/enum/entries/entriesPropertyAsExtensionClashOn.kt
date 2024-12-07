// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +EnumEntries -PrioritizedEnumEntries
// WITH_STDLIB
// FIR_DUMP

package pckg

enum class A {
    ;

    companion object
}

val A.Companion.entries: Int get() = 0

fun test() {
    A.<!DEBUG_INFO_CALL("fqName: pckg.entries; typeCall: variable"), DEPRECATED_ACCESS_TO_ENUM_ENTRY_COMPANION_PROPERTY!>entries<!>
    A.Companion.<!DEBUG_INFO_CALL("fqName: pckg.entries; typeCall: variable")!>entries<!>

    with(A) {
        this.entries
        <!DEBUG_INFO_CALL("fqName: pckg.entries; typeCall: variable")!>entries<!>
    }

    with(A.Companion) {
        <!DEBUG_INFO_CALL("fqName: pckg.entries; typeCall: variable")!>entries<!>
    }

    val aCompanion = A.Companion
    aCompanion.<!DEBUG_INFO_CALL("fqName: pckg.entries; typeCall: variable")!>entries<!>
}
