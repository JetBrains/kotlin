package test

public trait InheritReadOnliness: Object {

    public trait Super: Object {
        public fun <A: List<String>> foo(a: A)
    }

    public trait Sub: Super {
        override fun <B: List<String>> foo(a: B)
    }
}
