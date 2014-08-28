package test

public trait InheritNullabilityGenericSubclassSimple {

    public trait Super {
        public fun foo(): MutableCollection<String>

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super {
        override fun foo(): MutableList<String>
    }
}
