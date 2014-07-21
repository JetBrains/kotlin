package test

public trait InheritVarargInteger {

    public trait Super {
        public fun foo(vararg p0: Int?)

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super {
        override fun foo(vararg p0: Int?)
    }
}
