package test

public trait TwoTypeParameters {

    public trait Super {
        public fun <A: CharSequence, B: Cloneable> foo(a: A, b: B)
    }

    public trait Sub: Super {
        override fun <B: CharSequence, A: Cloneable> foo(a: B, b: A)
    }
}
