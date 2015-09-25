interface X
interface Y

fun X.iterator(): Y
fun Y.next(): Int
fun Y.hasNext(): Boolean


fun foo(x: X) {
    for (i in <caret>)
}

// ELEMENT: x
