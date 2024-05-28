sealed class Sealed(val value: String) {
    constructor() : this("OK")
}

class Derived : Sealed()

fun box() = Derived().value