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
        foo(x, _)
    }

    for ((_, y) in C()) {
        foo(_, y)
    }

    for ((_, _) in C()) {
        <!INAPPLICABLE_CANDIDATE!>foo<!>(_, _)
    }

    for ((_ : Int, _ : String) in C()) {
        <!INAPPLICABLE_CANDIDATE!>foo<!>(_, _)
    }

    for ((_ : String, _ : Int) in C()) {
        <!INAPPLICABLE_CANDIDATE!>foo<!>(_, _)
    }

    val (x, _) = A()
    val (_, y) = A()

    foo(x, y)
    <!INAPPLICABLE_CANDIDATE!>foo<!>(x, _)
    foo(_, y)

    val (`_`, z) = A()

    foo(_, z)

    val (_, `_`) = A()

    <!INAPPLICABLE_CANDIDATE!>foo<!>(_, y)

    val (unused, _) = A()
}

fun foo(x: Int, y: String) {}
