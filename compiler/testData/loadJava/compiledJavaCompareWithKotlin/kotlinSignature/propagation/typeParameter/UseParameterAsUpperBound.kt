package test

public trait UseParameterAsUpperBound: Object {

    public trait Super: Object {
        public fun <A, B: A> foo(a: A, b: B)
    }

    public trait Sub: Super {
        override fun <B, A: B> foo(a: B, b: A)
    }
}
