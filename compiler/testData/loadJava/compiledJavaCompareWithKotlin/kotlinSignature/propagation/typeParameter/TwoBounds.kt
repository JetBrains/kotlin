package test

public trait TwoBounds: Object {

    public trait Super: Object {
        public fun <A: CharSequence> foo(p0: A) where A: Cloneable
    }

    public trait Sub: Super {
        override fun <B: CharSequence> foo(p0: B) where B: Cloneable
    }
}
