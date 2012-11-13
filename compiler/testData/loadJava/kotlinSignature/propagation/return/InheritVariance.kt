package test

public trait InheritVariance: Object {

    public trait Super: Object {
        public fun foo(): MutableCollection<out Number>
    }

    public trait Sub: Super {
        override fun foo(): MutableList<out Number>
    }
}
