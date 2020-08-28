// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE
open class B<R1, R2>(x: R1, y: R2)

class A0<T1, T2> {
    constructor(x: T1, y: T2): <!CYCLIC_CONSTRUCTOR_DELEGATION_CALL!>this<!>(x, y)

    constructor(x: T1, y: T2, z: T2): this(x, 1) // ok, delegates to constructor(x: T1, y: Int)

    constructor(x: T1, y: Int): <!NONE_APPLICABLE!>this<!>(x, "")
    constructor(x: T1): this(x, 1)
    constructor(x: T1, y: T2, z: String): <!NONE_APPLICABLE!>this<!>(y, x)
}

class A1<T1, T2> : B<T1, T2> {
    constructor(x: T1, y: T2): super(x, y)
    constructor(x: T1, y: Int): <!INAPPLICABLE_CANDIDATE!>super<!>(x, y)
    constructor(x: T1, y: T1, z: T1): <!INAPPLICABLE_CANDIDATE!>super<!>(x, y)
}

class A2<T1, T2> : B<T1, Int> {
    constructor(x: T1, y: T2): <!INAPPLICABLE_CANDIDATE!>super<!>(x, y)
    constructor(x: T1, y: Int): super(x, y)
    constructor(x: T1, y: T1, z: T1): <!INAPPLICABLE_CANDIDATE!>super<!>(x, y)
    constructor(x: T1, y: T2, z: String): <!INAPPLICABLE_CANDIDATE!>super<!>(y, 1)
}

