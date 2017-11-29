// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

open class MyClass private constructor(val x: Int) {

    protected constructor(x: String) : this(x.length)

    constructor(x: Double) : this(x.toInt())
}

typealias MyAlias = MyClass

val test1 = <!OI;INVISIBLE_MEMBER!>MyAlias<!>(<!NI;CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
val test1a = <!OI;INVISIBLE_MEMBER!>MyClass<!>(<!NI;CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)

val test2 = <!OI;INVISIBLE_MEMBER!>MyAlias<!>(<!NI;TYPE_MISMATCH!>""<!>)
val test2a = <!OI;INVISIBLE_MEMBER!>MyClass<!>(<!NI;TYPE_MISMATCH!>""<!>)

val test3 = MyAlias(1.0)
val test3a = MyClass(1.0)

class MyDerived : MyClass(1.0) {
    val test4 = <!NI;NONE_APPLICABLE, OI;INVISIBLE_MEMBER!>MyAlias<!>(1)
    val test4a = <!NI;NONE_APPLICABLE, OI;INVISIBLE_MEMBER!>MyClass<!>(1)
    val test5 = <!PROTECTED_CONSTRUCTOR_NOT_IN_SUPER_CALL!>MyAlias<!>("")
    val test5a = <!OI;PROTECTED_CONSTRUCTOR_NOT_IN_SUPER_CALL!>MyClass<!>("")
    val test6 = MyAlias(1.0)
    val test6a = MyClass(1.0)
}