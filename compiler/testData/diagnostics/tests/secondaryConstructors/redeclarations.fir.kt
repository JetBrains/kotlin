// !DIAGNOSTICS: -UNUSED_PARAMETER

class A(x: String = "", y: String = "") {
    constructor(x: String, y: String): <!OVERLOAD_RESOLUTION_AMBIGUITY!>this<!>(x, y)
    constructor(): <!OVERLOAD_RESOLUTION_AMBIGUITY!>this<!>("", "")
    constructor(): <!OVERLOAD_RESOLUTION_AMBIGUITY!>this<!>("", "")
}

class B {
    <!CONFLICTING_OVERLOADS!>constructor(x: Int)<!>
}

<!CONFLICTING_OVERLOADS!>fun B(x: Int)<!> {}

class Outer {
    class A(x: String = "", y: String = "") {
        constructor(x: String, y: String): <!OVERLOAD_RESOLUTION_AMBIGUITY!>this<!>(x, y)
        constructor(): <!OVERLOAD_RESOLUTION_AMBIGUITY!>this<!>("", "")
        constructor(): <!OVERLOAD_RESOLUTION_AMBIGUITY!>this<!>("", "")
    }

    class B {
        constructor(x: Int)
    }

    fun B(x: Int) {}
}
