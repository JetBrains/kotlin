package test

public trait InheritVarargPrimitive: Object {

    public trait Super: Object {
        public fun foo(vararg p0: Int)

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super {
        override fun foo(vararg p0: Int)
    }
}
