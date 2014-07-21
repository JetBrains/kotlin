package test

public trait InheritNotVarargNotNull {

    public trait Super {
        public fun foo(p: Array<out String>)

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super {
        override fun foo(p: Array<out String>)
    }
}
