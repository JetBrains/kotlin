package test

public trait NullableToNotNullKotlinSignature: Object {

    public trait Super: Object {
        public fun foo(p: String?)

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super {
        override fun foo(p: String?)
    }
}
