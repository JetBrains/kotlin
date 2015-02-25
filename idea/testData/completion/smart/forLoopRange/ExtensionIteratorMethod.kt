trait X
trait Y

fun X.iterator(): Y
fun Y.next(): Int
fun Y.hasNext(): Boolean


fun foo(x: X, y: Y) {
    for (i in <caret>)
}

// EXIST: x
// ABSENT: y
