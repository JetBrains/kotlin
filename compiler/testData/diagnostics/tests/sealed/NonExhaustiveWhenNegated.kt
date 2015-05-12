sealed class Sealed(val x: Int) {
    object First: Sealed(12)
    open class NonFirst(x: Int, val y: Int): Sealed(x) {
        object Second: NonFirst(34, 2)
        object Third: NonFirst(56, 3)
    }
}

fun foo(s: Sealed): Int {
    return <!NO_ELSE_IN_WHEN!>when<!>(s) {
        !is Sealed.NonFirst -> 1
    }
}

