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

    for ((<!NI;TYPE_MISMATCH!>_ : String<!>, <!NI;TYPE_MISMATCH!>_ : Int<!>) in <!OI;COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH, OI;COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>C()<!>) {
        foo(<!UNRESOLVED_REFERENCE!>_<!>, <!UNRESOLVED_REFERENCE!>_<!>)
    }

    val (x, _) = A()
    val (_, y) = A()

    foo(x, y)
    foo(x, <!UNRESOLVED_REFERENCE!>_<!>)
    foo(<!UNRESOLVED_REFERENCE!>_<!>, y)

    val (<!REDECLARATION!>`_`<!>, z) = A()

    foo(<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>, z)

    val (_, <!NAME_SHADOWING, REDECLARATION!>`_`<!>) = A()

    foo(<!TYPE_MISMATCH, UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>, y)

    val (<!UNUSED_VARIABLE!>unused<!>, _) = A()
}

fun foo(<!UNUSED_PARAMETER!>x<!>: Int, <!UNUSED_PARAMETER!>y<!>: String) {}
