// !DIAGNOSTICS: -UNUSED_PARAMETER

class A(x: String = "", y: String = "") {
    constructor(x: String, y: String): <!AMBIGUITY!>this<!>(x, y)
    constructor(): <!AMBIGUITY!>this<!>("", "")
    constructor(): <!AMBIGUITY!>this<!>("", "")
}

class B {
    constructor(x: Int)
}

fun B(x: Int) {}

class Outer {
    class A(x: String = "", y: String = "") {
        constructor(x: String, y: String): <!AMBIGUITY!>this<!>(x, y)
        constructor(): <!AMBIGUITY!>this<!>("", "")
        constructor(): <!AMBIGUITY!>this<!>("", "")
    }

    class B {
        constructor(x: Int)
    }

    fun B(x: Int) {}
}
