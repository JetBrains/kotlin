// !DIAGNOSTICS: -UNUSED_PARAMETER
open class B0(x: Int)

class A0 : B0 {
    <!INAPPLICABLE_CANDIDATE!>constructor()<!>
    constructor(x: Int) : <!INAPPLICABLE_CANDIDATE!>super<!>()
}

// --------------------------

open class B1 {
    constructor(x: Int = 1)
    constructor()
}

class A1 : B1 {
    constructor()
    constructor(x: Int) : super()
}

// --------------------------

open class B2 {
    constructor(x: Int)
    constructor(x: String)
}

class A2 : B2 {
    <!EXPLICIT_DELEGATION_CALL_REQUIRED, NONE_APPLICABLE!>constructor()<!>
    constructor(x: Int) : <!NONE_APPLICABLE!>super<!>()
}

// --------------------------

open class B3 {
    private constructor()
}

class A3 : B3 {
    <!HIDDEN!>constructor()<!>
    constructor(x: Int) : <!HIDDEN!>super<!>()
}
