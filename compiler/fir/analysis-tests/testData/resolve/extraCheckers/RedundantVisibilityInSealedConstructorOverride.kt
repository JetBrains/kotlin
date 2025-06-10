// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND

sealed class A(val x: Int) {
    protected constructor(s: String) : this(s.length)

    class A1 : A(100)
    class A2 : A("String")
}


sealed class B protected constructor(val x: Int) {
    protected constructor(s: String) : this(s.length)

    class B1 : B(100)
    class B2 : B("String")
}

sealed class C <!REDUNDANT_VISIBILITY_MODIFIER!>private<!> constructor(val x: Int) {
    protected constructor(s: String) : this(s.length)

    class C1 : C(100)
    class C2 : C("String")
}

sealed class C3 : <!INVISIBLE_REFERENCE!>C<!>(200)

sealed class D {
    protected constructor(x: Int)
    protected constructor(s: String) : this(s.length)

    class D1 : D(100)
    class D2 : D("String")
}

sealed class E() {
    <!REDUNDANT_VISIBILITY_MODIFIER!>private<!> constructor(x: Int) : this()
    protected constructor(x: Byte) : this()
    <!NON_PRIVATE_OR_PROTECTED_CONSTRUCTOR_IN_SEALED!>internal constructor(x: Short) : this()<!>
    <!NON_PRIVATE_OR_PROTECTED_CONSTRUCTOR_IN_SEALED!>public constructor(x: Long) : this()<!>
    constructor(x: Double) : this()
}
