package test

public trait TwoBounds {

    public trait Super {
        public fun <A: CharSequence> foo(a: A) where A: Cloneable
    }

    public trait Sub: Super {
        override fun <B: CharSequence> foo(a: B) where B: Cloneable
    }
}
