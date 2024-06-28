val Int.plusAssign: (Int) -> Unit
    get() = {}

fun main() {
    1 <!NOT_FUNCTION_AS_OPERATOR!>+=<!> 2
}
