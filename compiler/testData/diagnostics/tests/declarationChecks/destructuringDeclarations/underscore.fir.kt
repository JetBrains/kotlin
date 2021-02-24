// !WITH_NEW_INFERENCE

class A {
    operator fun component1() = 1
    operator fun component2() = ""
}

class C {
    operator fun iterator(): Iterator<A> = null!!
}

fun test() {
    for ((x, _) in C()) {
        foo(x, <!UNRESOLVED_REFERENCE!>_<!>)
    }

    for ((_, y) in C()) {
        foo(<!UNRESOLVED_REFERENCE!>_<!>, y)
    }

    for ((_, _) in C()) {
        foo(<!UNRESOLVED_REFERENCE!>_<!>, <!UNRESOLVED_REFERENCE!>_<!>)
    }

    for ((_ : Int, _ : String) in C()) {
        foo(<!UNRESOLVED_REFERENCE!>_<!>, <!UNRESOLVED_REFERENCE!>_<!>)
    }

    for ((_ : String, _ : Int) in C()) {
        foo(<!UNRESOLVED_REFERENCE!>_<!>, <!UNRESOLVED_REFERENCE!>_<!>)
    }

    val (x, _) = A()
    val (_, y) = A()

    foo(x, y)
    foo(x, <!UNRESOLVED_REFERENCE!>_<!>)
    foo(<!UNRESOLVED_REFERENCE!>_<!>, y)

    val (`_`, z) = A()

    foo(_, z)

    val (_, `_`) = A()

    <!INAPPLICABLE_CANDIDATE!>foo<!>(_, y)

    val (unused, _) = A()
}

fun foo(x: Int, y: String) {}
