package test

public trait InheritNotVarargInteger: Object {

    public trait Super: Object {
        public fun foo(p0: Array<out Int>?)

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super {
        override fun foo(p0: Array<out Int>?)
    }
}
