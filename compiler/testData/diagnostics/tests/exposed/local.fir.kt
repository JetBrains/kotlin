// invalid, depends on local class
<!EXPOSED_FUNCTION_RETURN_TYPE!>fun foo() = run {
    class A
    A()
}<!>

// invalid, depends on local class
<!EXPOSED_FUNCTION_RETURN_TYPE!>fun gav() = {
    class B
    B()
}<!>

abstract class My

// valid, object literal here is effectively My
fun bar() = run {
    object: My() {}
}