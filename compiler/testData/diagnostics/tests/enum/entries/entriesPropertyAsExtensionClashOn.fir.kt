// !LANGUAGE: +EnumEntries
// WITH_STDLIB

package pckg

enum class A {
    ;

    companion object
}

val A.Companion.entries: Int get() = 0

fun test() {
    A.<!OPT_IN_USAGE_ERROR!>entries<!>
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
