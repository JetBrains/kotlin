package test

public trait InheritProjectionKind: Object {

    public trait Super: Object {
        public fun foo(): MutableCollection<out Number>
    }

    public trait Sub: Super {
        override fun foo(): MutableList<out Number>
    }
}
