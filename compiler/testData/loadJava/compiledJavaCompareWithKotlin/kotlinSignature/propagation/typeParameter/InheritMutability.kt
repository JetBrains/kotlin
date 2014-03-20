package test

public trait InheritMutability: Object {

    public trait Super: Object {
        public fun <A: MutableList<String>> foo(a: A)
    }

    public trait Sub: Super {
        override fun <B: MutableList<String>> foo(a: B)
    }
}
