sealed class Sealed {
    object First: Sealed()
    sealed class NonFirst {
        object Second: NonFirst()
        object Third: NonFirst()
        object Fourth: Sealed()
    }    
}

fun foo(s: Sealed, nf: Sealed.NonFirst): Int {
    val si = when(s) {
        Sealed.First -> 1
        Sealed.NonFirst.Fourth -> 4
    }
    val nfi = when(nf) {
        Sealed.NonFirst.Second -> 2
        Sealed.NonFirst.Third -> 3
    }
    return si + nfi
}
