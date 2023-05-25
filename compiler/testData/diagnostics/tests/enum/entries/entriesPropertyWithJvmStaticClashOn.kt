// !LANGUAGE: +EnumEntries -PrioritizedEnumEntries
// WITH_STDLIB
// FIR_DUMP

enum class A {
    ;

    companion object {
        @JvmStatic
        val entries = 0
    }
}

fun test() {
    A.<!DEBUG_INFO_CALL("fqName: A.Companion.entries; typeCall: variable"), DEPRECATED_ACCESS_TO_ENUM_ENTRY_COMPANION_PROPERTY!>entries<!>

    with(A) {
        <!DEBUG_INFO_CALL("fqName: A.Companion.entries; typeCall: variable")!>entries<!>
    }

    A.Companion.<!DEBUG_INFO_CALL("fqName: A.Companion.entries; typeCall: variable")!>entries<!>
}
