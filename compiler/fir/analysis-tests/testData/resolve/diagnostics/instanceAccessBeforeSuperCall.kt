class A {
    constructor(x: Int = <!UNRESOLVED_REFERENCE!>getSomeInt<!>(), other: A = <!NO_THIS!>this<!>, header: String = <!UNRESOLVED_REFERENCE!>keker<!>) {}
    fun getSomeInt() = 10
    var keker = "test"
}

class B(other: B = <!NO_THIS!>this<!>)

class C() {
    constructor(x: Int) : <!INAPPLICABLE_CANDIDATE!>this<!>({
        val a = 10
        this
    }) {}
}

class D {
    var a = 20
    constructor() {
        this.a = 10
    }
}

fun main() {
    val x1: String.() -> String = if (true) {
        { <!NO_THIS!>this<!> }
    } else {
        { <!NO_THIS!>this<!> }
    }
}