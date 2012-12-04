package test

public trait UseParameterInUpperBoundWithKotlinSignature: Object {

    public trait Super: Object {
        public fun <A, B: List<A>> foo(p0: A, p1: B)
    }

    public trait Sub: Super {
        override fun <B, A: List<B>> foo(p0: B, p1: A)
    }
}
