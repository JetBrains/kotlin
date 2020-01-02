// !DIAGNOSTICS: -UNUSED_PARAMETER

fun testCompound() {
    operator fun Nothing.get(i: Int) {}
    todo()!![12]
}

fun testCompound1() {
    operator fun Int.times(s: String): Array<String> = throw Exception()
    (todo() * "")[1]
}

fun todo(): Nothing = throw Exception()