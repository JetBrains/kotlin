// !LANGUAGE: -EnumEntries
// WITH_STDLIB

enum class A {
    ;

    companion object {
        @JvmStatic
        val entries = 0
    }
}

fun test() {
    A.entries

    with(A) {
        entries
    }

    A.Companion.entries
}
