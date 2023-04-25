
class A {
    operator fun component1() = 1
    operator fun component2() = ""
}

class C {
    operator fun iterator(): Iterator<A> = null!!
}

fun test() {
    for ((x, _) in C()) {
        foo(x, <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS, UNRESOLVED_REFERENCE!>_<!>)
    }

    for ((_, y) in C()) {
        foo(<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS, UNRESOLVED_REFERENCE!>_<!>, y)
    }

    for ((_, _) in C()) {
        foo(<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS, UNRESOLVED_REFERENCE!>_<!>, <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS, UNRESOLVED_REFERENCE!>_<!>)
    }

    for ((_ : Int, _ : String) in C()) {
        foo(<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS, UNRESOLVED_REFERENCE!>_<!>, <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS, UNRESOLVED_REFERENCE!>_<!>)
    }

    for ((_ : String, _ : Int) in C()) {
        foo(<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS, UNRESOLVED_REFERENCE!>_<!>, <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS, UNRESOLVED_REFERENCE!>_<!>)
    }

    val (x, _) = A()
    val (_, y) = A()

    foo(x, y)
    foo(x, <!UNDERSCORE_USAGE_WITHOUT_BACKTICKS, UNRESOLVED_REFERENCE!>_<!>)
    foo(<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS, UNRESOLVED_REFERENCE!>_<!>, y)

    val (<!REDECLARATION!>`_`<!>, z) = A()

    foo(<!UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>, z)

    val (_, <!REDECLARATION!>`_`<!>) = A()

    foo(<!ARGUMENT_TYPE_MISMATCH, UNDERSCORE_USAGE_WITHOUT_BACKTICKS!>_<!>, y)

    val (unused, _) = A()
}

fun foo(x: Int, y: String) {}
