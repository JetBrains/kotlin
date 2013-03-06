package test

public trait ChangeProjectionKind1: Object {

    public trait Super: Object {
        public fun foo(p0: MutableList<in String>)
    }

    public trait Sub: Super {
        override fun foo(p0: MutableList<in String>)
    }
}