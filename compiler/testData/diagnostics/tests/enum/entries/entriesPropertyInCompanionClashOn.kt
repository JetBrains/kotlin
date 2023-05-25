// !LANGUAGE: +EnumEntries -PrioritizedEnumEntries
// WITH_STDLIB
// FIR_DUMP

enum class A {
    ;

    companion object {
        val entries = 0
    }
}

fun test() {
    A.<!DEBUG_INFO_CALL("fqName: A.Companion.entries; typeCall: variable"), DEPRECATED_ACCESS_TO_ENUM_ENTRY_COMPANION_PROPERTY!>entries<!>
    A.Companion.<!DEBUG_INFO_CALL("fqName: A.Companion.entries; typeCall: variable")!>entries<!>

    with(A) {
        <!DEBUG_INFO_CALL("fqName: A.Companion.entries; typeCall: variable")!>entries<!>
        this.entries
        <!UNRESOLVED_REFERENCE!>values<!>() // to be sure that we don't resolve into synthetic 'values'
    }

    with(A.Companion) {
        <!DEBUG_INFO_CALL("fqName: A.Companion.entries; typeCall: variable")!>entries<!>
    }

    val aCompanion = A.Companion
    aCompanion.<!DEBUG_INFO_CALL("fqName: A.Companion.entries; typeCall: variable")!>entries<!>
}
