package f

class Progression<T>(val start: T, val end: T)

operator fun <T> Progression<T>.iterator() = object : Iterator<T> {
    var returned = false
    override fun hasNext() = !returned
    override fun next(): T {
        returned = true
        return this@iterator.start
    }
}

fun usage() {
    for (i <caret>in Progression<String>("a", "z")) {

    }
}
