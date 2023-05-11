// !LANGUAGE: -EnumEntries
// WITH_STDLIB

package pckg

enum class A {
    ;

    companion object
}

val <T> T.entries: Int get() = 0

fun test() {
    A.entries
    A.Companion.entries
}
