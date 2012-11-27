package test

public trait TwoSuperclasses: Object {

    public trait Super1: Object {
        public fun <A: CharSequence> foo(p0: A)
    }

    public trait Super2: Object {
        public fun <B: CharSequence> foo(p0: B)
    }

    public trait Sub: Super1, Super2 {
        override fun <C: CharSequence> foo(p0: C)
    }
}
