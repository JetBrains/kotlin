package test

public trait InheritMutability {

    public trait Super {
        public fun <A: MutableList<String>> foo(a: A)
    }

    public trait Sub: Super {
        override fun <B: MutableList<String>> foo(a: B)
    }
}
