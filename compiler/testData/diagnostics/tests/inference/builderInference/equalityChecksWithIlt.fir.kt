// WITH_RUNTIME

fun main(x: Long, y: Int) {
    sequence {
        1L == 3
        x == 3
        3 == 1L
        3 == x
        y == x

        1L === 3
        x === 3
        3 === 1L
        3 === x
        y === x

        yield("")
    }
}