package test

public trait InheritProjectionKind: Object {

    public trait Super: Object {
        public fun foo(p0: MutableList<in String>)
    }

    public trait Sub: Super {
        override fun foo(p0: MutableList<in String>)
    }
}
