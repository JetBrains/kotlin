package test

public trait SameProjectionKind {

    public trait Super {
        public fun foo(): MutableCollection<out Number?>?

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super {
        override fun foo(): MutableCollection<out Number?>?
    }
}
