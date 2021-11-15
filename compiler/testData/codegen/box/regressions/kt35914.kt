// WITH_STDLIB

class Inv<T>
fun <T> bar(x: Inv<T>.() -> Unit) = x

fun box(): String {
    listOf(
        bar<Char> { },
        bar { } // the problem is here
    )
    return "OK"
}
