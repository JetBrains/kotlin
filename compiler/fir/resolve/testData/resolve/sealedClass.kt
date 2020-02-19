sealed class Foo(val value: String)

class Bar : Foo("OK")

sealed class WithPrivateConstructor private constructor(val x: Int) {
    private constructor() : this(42)
}

object First : <!INAPPLICABLE_CANDIDATE!>WithPrivateConstructor<!>() // error
object Second : <!INAPPLICABLE_CANDIDATE!>WithPrivateConstructor<!>(0) // error