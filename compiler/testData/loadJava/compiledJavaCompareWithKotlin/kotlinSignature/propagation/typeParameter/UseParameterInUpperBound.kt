package test

public trait UseParameterInUpperBound {

    public trait Super {
        public fun <A, B: List<A>> foo(a: A, b: B)
    }

    public trait Sub: Super {
        override fun <B, A: List<B>> foo(a: B, b: A)
    }
}
