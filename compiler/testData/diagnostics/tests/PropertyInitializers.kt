class Foo(val a: Int, b: Int) {
    val c = a + b

    val d: Int
        get() = a

    val e: Int
        get() = <!UNRESOLVED_REFERENCE!>b<!>
}