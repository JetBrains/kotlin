package test

public trait SameProjectionKind: Object {

    public trait Super: Object {
        public fun foo(): MutableCollection<out Number?>?
    }

    public trait Sub: Super {
        override fun foo(): MutableCollection<out Number?>?
    }
}
