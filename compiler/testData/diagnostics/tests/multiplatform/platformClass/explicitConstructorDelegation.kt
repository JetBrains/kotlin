// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt
platform open class A {
    constructor(s: String)

    constructor(n: Number) : <!PLATFORM_CLASS_CONSTRUCTOR_DELEGATION_CALL!>this<!>("A")
}

platform class B : A {
    constructor(i: Int)

    constructor() : <!PLATFORM_CLASS_CONSTRUCTOR_DELEGATION_CALL!>super<!>("B")
}
