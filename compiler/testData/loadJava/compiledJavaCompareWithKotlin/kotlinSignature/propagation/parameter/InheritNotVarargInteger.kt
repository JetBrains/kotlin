package test

public trait InheritNotVarargInteger {

    public trait Super {
        public fun foo(p0: Array<out Int>?)

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super {
        override fun foo(p0: Array<out Int>?)
    }
}
