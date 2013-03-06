package test

public trait InheritMutability: Object {

    public trait Super: Object {
        public fun <A: MutableList<String>> foo(p0: A)
    }

    public trait Sub: Super {
        override fun <B: MutableList<String>> foo(p0: B)
    }
}
