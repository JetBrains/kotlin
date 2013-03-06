package test

public trait UseParameterAsUpperBound: Object {

    public trait Super: Object {
        public fun <A, B: A> foo(p0: A, p1: B)
    }

    public trait Sub: Super {
        override fun <B, A: B> foo(p0: B, p1: A)
    }
}
