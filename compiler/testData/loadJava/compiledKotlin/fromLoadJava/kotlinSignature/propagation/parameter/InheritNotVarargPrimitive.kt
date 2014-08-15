package test

public trait InheritNotVarargPrimitive {

    public trait Super {
        public fun foo(p0: IntArray?)

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super {
        override fun foo(p0: IntArray?)
    }
}
