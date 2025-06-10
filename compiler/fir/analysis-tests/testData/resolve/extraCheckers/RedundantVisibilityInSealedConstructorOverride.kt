// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// KT-76629

sealed class A(val x: Int) {
    <!REDUNDANT_VISIBILITY_MODIFIER!>protected<!> constructor(s: String) : this(s.length)

    class A1 : A(100)
    class A2 : A("String")
}


sealed class B <!REDUNDANT_VISIBILITY_MODIFIER!>protected<!> constructor(val x: Int) {
    <!REDUNDANT_VISIBILITY_MODIFIER!>protected<!> constructor(s: String) : this(s.length)

    class B1 : B(100)
    class B2 : B("String")
}

sealed class C private constructor(val x: Int) {
    <!REDUNDANT_VISIBILITY_MODIFIER!>protected<!> constructor(s: String) : this(s.length)

    class C1 : C(100)
    class C2 : C("String")
}

sealed class C3 : <!INVISIBLE_REFERENCE!>C<!>(200)

sealed class D {
    <!REDUNDANT_VISIBILITY_MODIFIER!>protected<!> constructor(x: Int)
    <!REDUNDANT_VISIBILITY_MODIFIER!>protected<!> constructor(s: String) : this(s.length)

    class D1 : D(100)
    class D2 : D("String")
}

sealed class E() {
    private constructor(x: Int) : this()
    <!REDUNDANT_VISIBILITY_MODIFIER!>protected<!> constructor(x: Byte) : this()
    <!NON_PRIVATE_OR_PROTECTED_CONSTRUCTOR_IN_SEALED!>internal constructor(x: Short) : this()<!>
    <!NON_PRIVATE_OR_PROTECTED_CONSTRUCTOR_IN_SEALED!>public constructor(x: Long) : this()<!>
    constructor(x: Double) : this()
}