package test

public trait InheritNullability: Object {

    public trait Super: Object {
        public fun <A: CharSequence> foo(p0: A)
    }

    public trait Sub: Super {
        override fun <B: CharSequence> foo(p0: B)
    }
}
