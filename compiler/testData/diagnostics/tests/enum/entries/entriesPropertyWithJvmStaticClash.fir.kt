// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -EnumEntries, -PrioritizedEnumEntries
// WITH_STDLIB

enum class A {
    ;

    companion object {
        @JvmStatic
        val entries = 0
    }
}

fun test() {
    <!DEPRECATED_ACCESS_TO_ENUM_ENTRY_COMPANION_PROPERTY!>A.entries<!>

    with(A) {
        entries
    }

    A.Companion.entries
}
