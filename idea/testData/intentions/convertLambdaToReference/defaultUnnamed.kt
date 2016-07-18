class Transformer {
    fun transform(x: Int = 0, y: Int = 1, f: (Int) -> Int) = f(x + y)
}

fun bar(x: Int) = x * x

val y = Transformer().transform(2) { <caret>bar(it) }
