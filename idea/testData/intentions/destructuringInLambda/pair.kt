// WITH_RUNTIME

fun foo() = 0 to 10

fun bar(): Int {
    val <caret>b = foo()
    return b.first + b.second
}