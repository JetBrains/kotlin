sealed class Sealed() {
    object First: Sealed()
    open class NonFirst: Sealed() {
        class NonSecond: NonFirst() {
            object Third: Sealed()
            class NonThird: Sealed() {
                object Fourth: NonFirst()
                class Fifth: Sealed()
            }    
        }
        object Second: Sealed()
    }    
}

fun foo(s: Sealed) = when(s) {
    Sealed.First -> 1
    is Sealed.NonFirst -> 2
    Sealed.NonFirst.Second -> 4
    Sealed.NonFirst.NonSecond.Third -> 6
    is Sealed.NonFirst.NonSecond.NonThird -> 8
    is Sealed.NonFirst.NonSecond.NonThird.Fifth -> 10
    // no else required
}

fun fooWithElse(s: Sealed) = when(s) {
    Sealed.First -> 1
    Sealed.NonFirst.NonSecond.Third -> 6
    is Sealed.NonFirst.NonSecond.NonThird.Fifth -> 10
    else -> 0
}

fun fooWithoutElse(s: Sealed) = <!NO_ELSE_IN_WHEN!>when<!>(s) {
    Sealed.First -> 1
    is Sealed.NonFirst -> 2
    Sealed.NonFirst.NonSecond.Third -> 6
    is Sealed.NonFirst.NonSecond.NonThird -> 8
    is Sealed.NonFirst.NonSecond.NonThird.Fifth -> 10
}

fun barWithoutElse(s: Sealed) = <!NO_ELSE_IN_WHEN!>when<!>(s) {
    Sealed.First -> 1
    is Sealed.NonFirst -> 2
    Sealed.NonFirst.Second -> 4
    is Sealed.NonFirst.NonSecond.NonThird -> 8
    is Sealed.NonFirst.NonSecond.NonThird.Fifth -> 10
}

