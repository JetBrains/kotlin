package test

public trait InheritNullabilityGenericSubclassSimple: Object {

    public trait Super: Object {
        public fun foo(): MutableCollection<String>
    }

    public trait Sub: Super {
        override fun foo(): MutableList<String>
    }
}
