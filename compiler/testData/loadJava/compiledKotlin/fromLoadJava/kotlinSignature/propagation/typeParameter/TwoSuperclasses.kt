package test

public trait TwoSuperclasses {

    public trait Super1 {
        public fun <A: CharSequence> foo(a: A)
    }

    public trait Super2 {
        public fun <B: CharSequence> foo(a: B)
    }

    public trait Sub: Super1, Super2 {
        override fun <C: CharSequence> foo(a: C)
    }
}
