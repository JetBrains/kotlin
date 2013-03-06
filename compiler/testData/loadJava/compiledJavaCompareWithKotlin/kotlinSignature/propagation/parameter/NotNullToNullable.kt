package test

public trait NotNullToNullable: Object {

    public trait Super: Object {
        public fun foo(p0: String)
    }

    public trait Sub: Super {
        override fun foo(p0: String)
    }
}