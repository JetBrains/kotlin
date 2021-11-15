// WITH_STDLIB
fun foo() {
    val array = Array(0, { IntArray(0) } )
    array.forEach { println(it.asList()) }
}

fun box(): String {
    foo() // just to be sure, that no exception happens
    return "OK"
}
