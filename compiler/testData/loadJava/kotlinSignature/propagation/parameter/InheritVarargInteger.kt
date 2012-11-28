package test

public trait InheritVarargInteger: Object {

    public trait Super: Object {
        public fun foo(vararg p0: Int?)
    }

    public trait Sub: Super {
        override fun foo(vararg p0: Int?)
    }
}
