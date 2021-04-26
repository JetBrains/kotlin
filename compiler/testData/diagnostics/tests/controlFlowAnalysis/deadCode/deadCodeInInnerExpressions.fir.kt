// !DIAGNOSTICS: -UNUSED_PARAMETER

fun testCompound() {
    operator fun Nothing.get(i: Int) {}
    todo()<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>[12]
}

fun testCompound1() {
    operator fun Int.times(s: String): Array<String> = throw Exception()
    (todo() * "")[1]
}

fun todo(): Nothing = throw Exception()
