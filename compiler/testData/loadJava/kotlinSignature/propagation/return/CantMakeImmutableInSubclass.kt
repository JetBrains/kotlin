package test

public trait CantMakeImmutableInSubclass: Object {

    public trait Super: Object {
        public fun foo(): MutableCollection<String>
    }

    public trait Sub: Super {
        override fun foo(): MutableList<String>
    }
}
