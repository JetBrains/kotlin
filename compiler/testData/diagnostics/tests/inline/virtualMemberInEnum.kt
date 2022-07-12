enum class Some {
    A {
        override fun foo(s: () -> String): String {
            return s() + s()
        }
    };

    //SHOULD BE ERROR REPORTED
    open <!DECLARATION_CANT_BE_INLINED_WARNING!>inline<!> fun foo(s: () -> String): String {
        return s()
    }
}
