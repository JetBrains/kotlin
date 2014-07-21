package test

public trait InheritNullability {

    public trait Super {
        public fun <A: CharSequence> foo(a: A)
    }

    public trait Sub: Super {
        override fun <B: CharSequence> foo(a: B)
    }
}
