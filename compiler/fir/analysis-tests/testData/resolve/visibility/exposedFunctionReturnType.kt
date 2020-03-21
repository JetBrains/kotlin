class A {
    private class InnerA {

    }
}

abstract class B {
    <!EXPOSED_FUNCTION_RETURN_TYPE!>fun foo(str: String): A.InnerA<!>
}