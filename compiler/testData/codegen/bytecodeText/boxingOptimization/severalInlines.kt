
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

// JVM_TEMPLATES
// 1 LOCALVARIABLE x I (.*) 6
// 1 LOCALVARIABLE y I (.*) 5
// 1 LOCALVARIABLE x J (.*) 7
// 1 LOCALVARIABLE y J (.*) 5
// 1 LOCALVARIABLE x F (.*) 6
// 1 LOCALVARIABLE y F (.*) 5
// 1 LOCALVARIABLE x D (.*) 7
// 1 LOCALVARIABLE y D (.*) 5
// 1 LOCALVARIABLE x B (.*) 6
// 1 LOCALVARIABLE y B (.*) 5
// 1 LOCALVARIABLE x S (.*) 6
// 1 LOCALVARIABLE y S (.*) 5
// 1 LOCALVARIABLE x C (.*) 6
// 1 LOCALVARIABLE y C (.*) 5
// 1 LOCALVARIABLE x Z (.*) 6
// 1 LOCALVARIABLE y Z (.*) 5

// JVM_IR_TEMPLATES
// 1 LOCALVARIABLE x I (.*) 6
// 1 LOCALVARIABLE y I (.*) 5
// 1 LOCALVARIABLE x J (.*) 7
// 1 LOCALVARIABLE y J (.*) 5
// 1 LOCALVARIABLE x F (.*) 6
// 1 LOCALVARIABLE y F (.*) 5
// 1 LOCALVARIABLE x D (.*) 7
// 1 LOCALVARIABLE y D (.*) 5
// 1 LOCALVARIABLE x B (.*) 6
// 1 LOCALVARIABLE y B (.*) 5
// 1 LOCALVARIABLE x S (.*) 6
// 1 LOCALVARIABLE y S (.*) 5
// 1 LOCALVARIABLE x C (.*) 6
// 1 LOCALVARIABLE y C (.*) 5
// 1 LOCALVARIABLE x Z (.*) 6
// 1 LOCALVARIABLE y Z (.*) 5

// JVM_IR_TEMPLATES_WITH_INLINE_SCOPES
// 1 LOCALVARIABLE x\\2 I (.*) 6
// 1 LOCALVARIABLE y\\2 I (.*) 5
// 1 LOCALVARIABLE x\\4 J (.*) 7
// 1 LOCALVARIABLE y\\4 J (.*) 5
// 1 LOCALVARIABLE x\\6 F (.*) 6
// 1 LOCALVARIABLE y\\6 F (.*) 5
// 1 LOCALVARIABLE x\\8 D (.*) 7
// 1 LOCALVARIABLE y\\8 D (.*) 5
// 1 LOCALVARIABLE x\\10 B (.*) 6
// 1 LOCALVARIABLE y\\10 B (.*) 5
// 1 LOCALVARIABLE x\\12 S (.*) 6
// 1 LOCALVARIABLE y\\12 S (.*) 5
// 1 LOCALVARIABLE x\\14 C (.*) 6
// 1 LOCALVARIABLE y\\14 C (.*) 5
// 1 LOCALVARIABLE x\\16 Z (.*) 6
// 1 LOCALVARIABLE y\\16 Z (.*) 5
