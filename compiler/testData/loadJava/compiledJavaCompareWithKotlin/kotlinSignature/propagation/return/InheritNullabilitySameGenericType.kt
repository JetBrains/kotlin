package test

public trait InheritNullabilitySameGenericType: Object {

    public trait Super: Object {
        public fun foo(): MutableList<String>
    }

    public trait Sub: Super {
        override fun foo(): MutableList<String>
    }
}