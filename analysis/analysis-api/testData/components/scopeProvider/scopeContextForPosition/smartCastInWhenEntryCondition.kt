class A

class B

private fun Any.test(): Int = when {
    this is A && <expr>a</expr> -> 10
    this is B && b -> 2
    else -> 0
}