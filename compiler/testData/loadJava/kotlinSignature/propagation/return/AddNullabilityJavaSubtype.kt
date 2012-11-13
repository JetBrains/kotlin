package test

public trait AddNullabilityJavaSubtype: Object {

    public trait Super: Object {
        public fun foo(): CharSequence
    }

    public trait Sub: Super {
        override fun foo(): String
    }
}