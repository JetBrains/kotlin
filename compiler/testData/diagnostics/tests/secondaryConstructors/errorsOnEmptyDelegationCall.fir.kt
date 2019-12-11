// !DIAGNOSTICS: -UNUSED_PARAMETER
open class B0(x: Int)

class A0 : B0 {
    constructor()
    constructor(x: Int) : super()
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
    constructor()
    constructor(x: Int) : super()
}

// --------------------------

open class B3 {
    private constructor()
}

class A3 : B3 {
    constructor()
    constructor(x: Int) : super()
}