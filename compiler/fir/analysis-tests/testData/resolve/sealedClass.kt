sealed class Foo(val value: String)

class Bar : Foo("OK")

sealed class WithPrivateConstructor private constructor(val x: Int) {
    private constructor() : this(42)
}

object First : <!NONE_APPLICABLE{LT}!><!NONE_APPLICABLE{PSI}!>WithPrivateConstructor<!>()<!> // error
object Second : <!NONE_APPLICABLE{LT}!><!NONE_APPLICABLE{PSI}!>WithPrivateConstructor<!>(0)<!> // error
