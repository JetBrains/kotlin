package test

public trait InheritVararg {

    public trait Super {
        public fun foo(vararg p0: String?)

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super {
        override fun foo(vararg p0: String?)
    }
}
