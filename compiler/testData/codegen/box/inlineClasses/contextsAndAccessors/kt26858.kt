// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class Direction(private val direction: Int) {
    fun dx() = dx[direction]
    fun dy() = dy[direction]

    companion object {
        private val dx = intArrayOf(0, 1, 0, -1)
        private val dy = intArrayOf(-1, 0, 1, 0)
    }
}

fun box(): String {
    val dirs = arrayOf(Direction(0), Direction(1), Direction(2), Direction(3))
    val expectedDx = intArrayOf(0, 1, 0, -1)
    val expectedDy = intArrayOf(-1, 0, 1, 0)
    for (i in 0 .. 3) {
        if (dirs[i].dx() != expectedDx[i]) throw AssertionError()
        if (dirs[i].dy() != expectedDy[i]) throw AssertionError()
    }

    return "OK"
}