class A {
    private class InnerA {

    }
}

abstract class B {
    <!EXPOSED_FUNCTION_RETURN_TYPE{LT}, NON_ABSTRACT_FUNCTION_WITH_NO_BODY!>fun <!EXPOSED_FUNCTION_RETURN_TYPE{PSI}!>foo<!>(str: String): A.InnerA<!>
}

private enum class Some {
    FIRST {
        override fun foo(): Some = FIRST
    };

    abstract fun foo(): Some
}
