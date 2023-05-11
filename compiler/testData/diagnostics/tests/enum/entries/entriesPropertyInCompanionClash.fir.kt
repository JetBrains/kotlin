// !LANGUAGE: -EnumEntries
// WITH_STDLIB

enum class A {
    ;

    companion object {
        val entries = 0
    }
}

fun test() {
    A.entries
    A.Companion.entries

    with(A) {
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
