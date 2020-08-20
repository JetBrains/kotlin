class A {
    private class InnerA {

    }
}

abstract class B {
    fun <!EXPOSED_FUNCTION_RETURN_TYPE!>foo<!>(str: String): A.InnerA
}

private enum class Some {
    FIRST {
        override fun foo(): Some = FIRST
    };

    abstract fun foo(): Some
}
