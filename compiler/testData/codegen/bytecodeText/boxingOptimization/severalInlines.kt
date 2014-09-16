
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
// 1 LOCALVARIABLE x I (.*) 5
// 1 LOCALVARIABLE y I (.*) 4
// 1 LOCALVARIABLE x J (.*) 6
// 1 LOCALVARIABLE y J (.*) 4
// 1 LOCALVARIABLE x F (.*) 5
// 1 LOCALVARIABLE y F (.*) 4
// 1 LOCALVARIABLE x D (.*) 6
// 1 LOCALVARIABLE y D (.*) 4
// 1 LOCALVARIABLE x B (.*) 5
// 1 LOCALVARIABLE y B (.*) 4
// 1 LOCALVARIABLE x S (.*) 5
// 1 LOCALVARIABLE y S (.*) 4
// 1 LOCALVARIABLE x C (.*) 5
// 1 LOCALVARIABLE y C (.*) 4
// 1 LOCALVARIABLE x Z (.*) 5
// 1 LOCALVARIABLE y Z (.*) 4
