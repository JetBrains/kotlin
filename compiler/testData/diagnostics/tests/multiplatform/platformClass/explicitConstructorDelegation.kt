// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt
header open class A {
    constructor(s: String)

    constructor(n: Number) : <!HEADER_CLASS_CONSTRUCTOR_DELEGATION_CALL!>this<!>("A")
}

header class B : A {
    constructor(i: Int)

    constructor() : <!HEADER_CLASS_CONSTRUCTOR_DELEGATION_CALL!>super<!>("B")
}
