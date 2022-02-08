class A {
    private class InnerA {

    }
}

abstract class B {
    <!NON_ABSTRACT_FUNCTION_WITH_NO_BODY!>fun <!EXPOSED_FUNCTION_RETURN_TYPE!>foo<!>(str: String): <!INVISIBLE_REFERENCE!>A.InnerA<!><!>
}

private enum class Some {
    FIRST {
        override fun foo(): Some = FIRST
    };

    abstract fun foo(): Some
}
