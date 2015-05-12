package test

public interface NullableToNotNullKotlinSignature {

    public interface Super {
        public fun foo(p: String?)

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub: Super {
        override fun foo(p: String?)
    }
}
