// NI_EXPECTED_FILE

open class MyClass private constructor(val x: Int) {

    protected constructor(x: String) : this(x.length)

    constructor(x: Double) : this(x.toInt())
}

typealias MyAlias = MyClass

val test1 = <!INVISIBLE_REFERENCE!>MyAlias<!>(1)
val test1a = <!INVISIBLE_REFERENCE!>MyClass<!>(1)

val test2 = <!INVISIBLE_REFERENCE!>MyAlias<!>("")
val test2a = <!INVISIBLE_REFERENCE!>MyClass<!>("")

val test3 = MyAlias(1.0)
val test3a = MyClass(1.0)

class MyDerived : MyClass(1.0) {
    val test4 = <!INVISIBLE_REFERENCE!>MyAlias<!>(1)
    val test4a = <!INVISIBLE_REFERENCE!>MyClass<!>(1)
    val test5 = MyAlias("")
    val test5a = MyClass("")
    val test6 = MyAlias(1.0)
    val test6a = MyClass(1.0)
}
