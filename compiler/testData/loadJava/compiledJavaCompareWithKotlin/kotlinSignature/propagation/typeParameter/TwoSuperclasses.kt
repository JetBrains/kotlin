package test

public trait TwoSuperclasses: Object {

    public trait Super1: Object {
        public fun <A: CharSequence> foo(a: A)
    }

    public trait Super2: Object {
        public fun <B: CharSequence> foo(a: B)
    }

    public trait Sub: Super1, Super2 {
        override fun <C: CharSequence> foo(a: C)
    }
}
