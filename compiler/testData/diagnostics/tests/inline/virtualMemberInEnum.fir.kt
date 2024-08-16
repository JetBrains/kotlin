enum class Some {
    A {
        override <!OVERRIDE_BY_INLINE!>fun foo(s: () -> String): String<!> {
            return s() + s()
        }
    };

    //SHOULD BE ERROR REPORTED
    open <!DECLARATION_CANT_BE_INLINED!>inline<!> fun foo(s: () -> String): String {
        return s()
    }
}
