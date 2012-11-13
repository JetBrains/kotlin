package test

public trait AddNotNullJavaSubtype : Object {

    public trait Super: Object {
        public fun foo(): CharSequence?
    }

    public trait Sub: Super {
        override fun foo(): String
    }
}