sealed class Foo(val value: String)

class Bar : Foo("OK")

sealed class WithPrivateConstructor private constructor(val x: Int) {
    private constructor() : this(42)
}

object First : <!INVISIBLE_REFERENCE!>WithPrivateConstructor<!>() // error
object Second : <!INVISIBLE_REFERENCE!>WithPrivateConstructor<!>(0) // error
