package test

public trait InheritNotVararg: Object {

    public trait Super: Object {
        public fun foo(p0: Array<out String>?)

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super {
        override fun foo(p0: Array<out String>?)
    }
}
