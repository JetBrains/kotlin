package test

public trait InheritVarargNotNull {

    public trait Super {
        public fun foo(vararg p: String)

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super {
        override fun foo(vararg p: String)
    }
}
