package test

public trait InheritNullability: Object {

    public trait Super: Object {
        public fun foo(p0: String)
    }

    public trait Sub: Super {
        override fun foo(p0: String)
    }
}