// !LANGUAGE: +EnumEntries +PrioritizedEnumEntries
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
    A.<!DEPRECATED_ACCESS_TO_ENUM_ENTRY_COMPANION_PROPERTY!>entries<!>

    with(A) {
        entries
    }

    A.Companion.entries
}
