// IGNORE_REVERSED_RESOLVE
// !DUMP_CFG
fun getNothing(): Nothing = throw Exception()
fun getNullableNothing(): Nothing? = null

val String?.q: Int get() = 1
val String.qq: Int get() = 2

fun <T> myListOf(x: T): List<T> = null!!

class A {
    val a: Int = 1
    val b: Boolean = true
}

fun test_0(results: List<Nothing>) {
    var s: A? = null
    for (result in results) {
        s = result
        if (result.<!UNRESOLVED_REFERENCE!>b<!>) {
            break
        }
    }
    s?.let { it.a }
}

fun test_1(a: String?) {
    if (a is Nothing?) {
        val b = a?.length
    }

    if (a is Nothing) {
        val b = a.length
    }
}
