// !DIAGNOSTICS: -UNUSED_PARAMETER
open class B<R1, R2>(x: R1, y: R2)

class A0<T1, T2> {
    constructor(x: T1, y: T2): <!CYCLIC_CONSTRUCTOR_DELEGATION_CALL!>this<!>(x, y)

    constructor(x: T1, y: T2, z: T2): this(x, 1) // ok, delegates to constructor(x: T1, y: Int)

    constructor(x: T1, y: Int): this(x, <!ARGUMENT_TYPE_MISMATCH!>""<!>)
    constructor(x: T1): this(x, 1)
    constructor(x: T1, y: T2, z: String): this(<!ARGUMENT_TYPE_MISMATCH!>y<!>, <!ARGUMENT_TYPE_MISMATCH!>x<!>)
}

class A1<T1, T2> : B<T1, T2> {
    constructor(x: T1, y: T2): super(x, y)
    constructor(x: T1, y: Int): super(x, <!ARGUMENT_TYPE_MISMATCH!>y<!>)
    constructor(x: T1, y: T1, z: T1): super(x, <!ARGUMENT_TYPE_MISMATCH!>y<!>)
}

class A2<T1, T2> : B<T1, Int> {
    constructor(x: T1, y: T2): super(x, <!ARGUMENT_TYPE_MISMATCH!>y<!>)
    constructor(x: T1, y: Int): super(x, y)
    constructor(x: T1, y: T1, z: T1): super(x, <!ARGUMENT_TYPE_MISMATCH!>y<!>)
    constructor(x: T1, y: T2, z: String): super(<!ARGUMENT_TYPE_MISMATCH!>y<!>, 1)
}

