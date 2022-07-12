enum class Some {
    A {
        <!OVERRIDE_BY_INLINE!>override fun foo(s: () -> String): String<!> {
            return s() + s()
        }
    };

    //SHOULD BE ERROR REPORTED
    open <!DECLARATION_CANT_BE_INLINED!>inline<!> fun foo(s: () -> String): String {
        return s()
    }
}
