package test

public trait TwoTypeParameters: Object {

    public trait Super: Object {
        public fun <A: CharSequence, B: Cloneable> foo(p0: A, p1: B)
    }

    public trait Sub: Super {
        override fun <B: CharSequence, A: Cloneable> foo(p0: B, p1: A)
    }
}
