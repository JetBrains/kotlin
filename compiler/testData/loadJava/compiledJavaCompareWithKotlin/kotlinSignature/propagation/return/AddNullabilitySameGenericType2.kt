package test

public trait AddNullabilitySameGenericType2: Object {

    public trait Super: Object {
        public fun foo(): MutableList<String>

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super {
        override fun foo(): MutableList<String>
    }
}