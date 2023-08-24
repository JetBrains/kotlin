// !LANGUAGE: -EnumEntries
// WITH_STDLIB

package pckg

enum class A {
    ;

    companion object
}

val A.Companion.entries: Int get() = 0

fun test() {
    <!DEPRECATED_ACCESS_TO_ENUM_ENTRY_COMPANION_PROPERTY!>A.entries<!>
    A.Companion.entries

    with(A) {
        this.entries
        entries
    }

    with(A.Companion) {
        entries
    }

    val aCompanion = A.Companion
    aCompanion.entries
}
