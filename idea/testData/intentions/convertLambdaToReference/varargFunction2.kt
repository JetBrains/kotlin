// WITH_RUNTIME
fun test(vararg i: Int): IntArray {
    return i.let { <caret>double(*it) }
}

fun double(vararg elements: Int): IntArray = elements.map { it * 2 }.toIntArray()