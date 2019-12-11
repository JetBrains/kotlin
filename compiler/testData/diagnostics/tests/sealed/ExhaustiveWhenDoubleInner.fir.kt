sealed class Sealed() {
    object First: Sealed()
    open class NonFirst: Sealed() {
        object Second: NonFirst()
        object Third: NonFirst()
        // It's ALLOWED to inherit Sealed also from here
        object Fourth: Sealed()
    }    
}

fun foo(s: Sealed) = when(s) {
    Sealed.First -> 1
    is Sealed.NonFirst -> 2
    Sealed.NonFirst.Fourth -> 4
    // no else required
}
