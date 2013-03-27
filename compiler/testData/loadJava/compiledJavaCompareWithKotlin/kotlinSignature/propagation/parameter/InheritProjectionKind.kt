package test

public trait InheritProjectionKind: Object {

    public trait Super: Object {
        public fun foo(p0: MutableList<in String>)

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super {
        override fun foo(p0: MutableList<in String>)
    }
}
