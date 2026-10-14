package test

data class Point(val x: Int, val y: Int)

fun <T> block(obj: T, action: (T) -> Unit) {
    action(obj)
}

fun test() {
    block(Point(1, 2)) { (<expr>x</expr>, y) ->
        x + y
    }
}
