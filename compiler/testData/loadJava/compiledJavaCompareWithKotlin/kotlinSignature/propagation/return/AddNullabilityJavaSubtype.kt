package test

public trait AddNullabilityJavaSubtype: Object {

    public trait Super: Object {
        public fun foo(): CharSequence

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super {
        override fun foo(): String
    }
}