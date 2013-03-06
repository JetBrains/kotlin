package test

public trait InheritNotVarargPrimitive: Object {

    public trait Super: Object {
        public fun foo(p0: IntArray?)
    }

    public trait Sub: Super {
        override fun foo(p0: IntArray?)
    }
}
