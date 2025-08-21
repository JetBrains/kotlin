val i: Int = ""

fun foo(s: String): String {
    foo(i)
    return 1
}

val a: Array<Int> = arrayOf(1)
val b: Array<Int> = arrayOf(a)