package test

public trait InheritReadOnliness {

    public trait Super {
        public fun <A: List<String>> foo(a: A)
    }

    public trait Sub: Super {
        override fun <B: List<String>> foo(a: B)
    }
}
