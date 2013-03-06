package test

public trait InheritMutability: Object {

    public trait Super: Object {
        public fun foo(p0: MutableList<String>)
    }

    public trait Sub: Super {
        override fun foo(p0: MutableList<String>)
    }
}
