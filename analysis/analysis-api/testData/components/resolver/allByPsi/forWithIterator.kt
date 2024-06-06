package f

class IntProgression(val start: Int, val end: Int)

operator fun IntProgression.iterator() = object : Iterator<Int> {
    var current = this@iterator.start
    override fun hasNext() = current <= this@iterator.end
    override fun next(): Int = current++
}

fun usage() {
    for (i in IntProgression(1, 10)) {

    }
}