fun <T> run(f: () -> T): T {
    return f()
}

// invalid, depends on local class
fun <!FUNCTION_RETURN_TYPE_DEPENDS_ON_LOCAL_CLASS!>foo<!>() = run {
    class A
    A()
}

// invalid, depends on local class
fun <!FUNCTION_RETURN_TYPE_DEPENDS_ON_LOCAL_CLASS!>gav<!>() = {
    class B
    B()
}

abstract class My

// valid, object literal here is effectively My
fun bar() = run {
    object: My() {}
}