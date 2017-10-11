class Transformer {
    fun transform(x: Int = 0, y: Int = 1, f: (Int) -> Int) = f(x + y)
}

fun bar(x: Int) = x * x

val y = Transformer().transform(x = 2, y = 3) { <caret>bar(it) }
