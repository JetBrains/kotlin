// !LANGUAGE: +ArrayLiteralsInAnnotations
// !DIAGNOSTICS: -UNUSED_VARIABLE, -UNSUPPORTED

annotation class Anno(val a: Array<String> = [""], val b: IntArray = [])

@Anno([], [])
fun test() {}

fun arrayOf(): Array<Int> = TODO()
fun intArrayOf(): Array<Int> = TODO()

fun local() {
    val a1: IntArray = [1, 2]
    val a2: IntArray = []

    val s1: Array<String> = [""]
    val s2: Array<String> = []
}