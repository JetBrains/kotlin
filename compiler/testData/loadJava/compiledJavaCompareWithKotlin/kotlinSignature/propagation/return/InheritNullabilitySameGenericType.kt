package test

public trait InheritNullabilitySameGenericType {

    public trait Super {
        public fun foo(): MutableList<String>

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super {
        override fun foo(): MutableList<String>
    }
}
