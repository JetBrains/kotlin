// DO_NOT_CHECK_SYMBOL_RESTORE

class A
typealias B = A
typealias C = B
typealias D = C

fun x() {
    val a = <caret>B()
}
