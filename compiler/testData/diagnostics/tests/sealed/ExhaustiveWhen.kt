sealed class Sealed(val x: Int) {
    object First: Sealed(12)
    open class NonFirst(x: Int, val y: Int): Sealed(x) {
        object Second: NonFirst(34, 2)
        object Third: NonFirst(56, 3)
    }
}

fun foo(s: Sealed): Int {
    return when(s) {
        is Sealed.First -> 1
        is Sealed.NonFirst -> 0
        // no else required
    }
}

