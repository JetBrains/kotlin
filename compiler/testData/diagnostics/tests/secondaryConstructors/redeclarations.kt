// !DIAGNOSTICS: -UNUSED_PARAMETER

class A<!CONFLICTING_OVERLOADS!>(x: String = "", y: String = "")<!> {
    <!CONFLICTING_OVERLOADS!>constructor(x: String, y: String)<!>: <!OVERLOAD_RESOLUTION_AMBIGUITY!>this<!>(x, y)
    <!CONFLICTING_OVERLOADS!>constructor()<!>: <!OVERLOAD_RESOLUTION_AMBIGUITY!>this<!>("", "")
    <!CONFLICTING_OVERLOADS!>constructor()<!>: <!OVERLOAD_RESOLUTION_AMBIGUITY!>this<!>("", "")
}

class B {
    <!CONFLICTING_OVERLOADS!>constructor(x: Int)<!>
}

<!CONFLICTING_OVERLOADS!>fun B(x: Int)<!> {}

class Outer {
    class A<!CONFLICTING_OVERLOADS!>(x: String = "", y: String = "")<!> {
        <!CONFLICTING_OVERLOADS!>constructor(x: String, y: String)<!>: <!OVERLOAD_RESOLUTION_AMBIGUITY!>this<!>(x, y)
        <!CONFLICTING_OVERLOADS!>constructor()<!>: <!OVERLOAD_RESOLUTION_AMBIGUITY!>this<!>("", "")
        <!CONFLICTING_OVERLOADS!>constructor()<!>: <!OVERLOAD_RESOLUTION_AMBIGUITY!>this<!>("", "")
    }

    class B {
        <!CONFLICTING_OVERLOADS!>constructor(x: Int)<!>
    }

    <!CONFLICTING_OVERLOADS!>fun B(x: Int)<!> {}
}
