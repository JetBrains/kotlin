// FIR_IDENTICAL
sealed class Sealed {
    object First: Sealed()
    open class NonFirst: Sealed() {
        object Second: NonFirst()
        object Third: NonFirst()
    }
}

fun foo(s: Sealed): Int {
    return <!NO_ELSE_IN_WHEN!>when<!>(s) {
        is Sealed.First -> 1
        <!USELESS_IS_CHECK!>!is Any<!> -> 0
    }
}

