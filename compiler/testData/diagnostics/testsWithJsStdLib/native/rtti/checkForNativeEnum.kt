external enum Foo { A, B }

fun box(a: Any, b: Any): Boolean {
    return <!CANNOT_CHECK_FOR_EXTERNAL_ENUM!>a is Foo<!> && <!CANNOT_CHECK_FOR_EXTERNAL_ENUM!>b !is Foo<!>
}