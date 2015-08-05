interface I1
interface I2

fun foo(a: I1, b: I2, c: I1) {}

fun bar(b: I2, a: I1, c: I1) {
    foo(<caret>)
}

// ORDER: a
// ORDER: c
// ORDER: "a, b, c"
