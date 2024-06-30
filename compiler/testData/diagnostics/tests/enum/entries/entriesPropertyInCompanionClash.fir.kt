// LANGUAGE: -EnumEntries, -PrioritizedEnumEntries
// WITH_STDLIB

enum class A {
    ;

    companion object {
        val entries = 0
    }
}

fun test() {
    <!DEPRECATED_ACCESS_TO_ENUM_ENTRY_COMPANION_PROPERTY!>A.entries<!>
    A.Companion.entries

    <!CANNOT_INFER_PARAMETER_TYPE!>with<!>(A) {
        entries
        this.entries
        <!UNRESOLVED_REFERENCE!>values<!>() // to be sure that we don't resolve into synthetic 'values'
    }

    with(A.Companion) {
        entries
    }

    val aCompanion = A.Companion
    aCompanion.entries
}
