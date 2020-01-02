// !DIAGNOSTICS: -UNUSED_PARAMETER
open class B<X, Y : X> {
    constructor(x: X, y: Y)
    constructor(x: X, s: String)
    constructor(y: Y, i: Int) : this(y, "")
}

class A<T1, T2 : T1> : B<T1, T2> {
    constructor(x: T1, y: T2): <!INAPPLICABLE_CANDIDATE!>super<!>(x, y)
    constructor(x: T2, y: T2, z: String): <!INAPPLICABLE_CANDIDATE!>super<!>(x, y)

    constructor(x: T2, z: String, z1: String): <!INAPPLICABLE_CANDIDATE!>super<!>(x, "")
    constructor(x: T2, z: String, z1: String, z2: String): <!INAPPLICABLE_CANDIDATE!>super<!>(x, 1)
    constructor(x: T1, z: String, z1: String, z2: String, z3: String): <!INAPPLICABLE_CANDIDATE!>super<!>(x, "")
    constructor(x: T1, z: String, z1: String, z2: String, z3: String, z4: String): <!INAPPLICABLE_CANDIDATE!>super<!>(x, 1)
}
