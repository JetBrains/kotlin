// invalid, depends on local class
fun foo() = run {
    class A
    A()
}

// invalid, depends on local class
fun gav() = {
    class B
    B()
}

abstract class My

// valid, object literal here is effectively My
fun bar() = run {
    object: My() {}
}