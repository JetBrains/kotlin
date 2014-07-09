
inline fun <R, T> foo(x : R, y : R, block : (R, R) -> T) : T {
    return block(x, y)
}

fun bar() {
    foo(1, 2) { x, y -> x + y }
    foo(1L, 2L) { x, y -> x + y }
    foo(1f, 2f) { x, y -> x + y }
    foo(1.toDouble(), 2.toDouble()) { x, y -> x + y }
    foo(1.toByte(), 2.toByte()) { x, y -> x + y }
    foo(1.toShort(), 2.toShort()) { x, y -> x + y }
    foo('a', 'b') { x, y -> x == y }
    foo(true, false) { x, y -> x || y }
}

// 0 valueOf
// 0 Value\s\(\)
// 1 LOCALVARIABLE x I L6 L11 5
// 1 LOCALVARIABLE y I L6 L11 4
// 1 LOCALVARIABLE x J L19 L24 6
// 1 LOCALVARIABLE y J L19 L24 4
// 1 LOCALVARIABLE x F L32 L37 5
// 1 LOCALVARIABLE y F L32 L37 4
// 1 LOCALVARIABLE x D L45 L50 6
// 1 LOCALVARIABLE y D L45 L50 4
// 1 LOCALVARIABLE x B L58 L63 5
// 1 LOCALVARIABLE y B L58 L63 4
// 1 LOCALVARIABLE x S L71 L76 5
// 1 LOCALVARIABLE y S L71 L76 4
// 1 LOCALVARIABLE x C L84 L91 5
// 1 LOCALVARIABLE y C L84 L91 4
// 1 LOCALVARIABLE x Z L99 L106 5
// 1 LOCALVARIABLE y Z L99 L106 4
