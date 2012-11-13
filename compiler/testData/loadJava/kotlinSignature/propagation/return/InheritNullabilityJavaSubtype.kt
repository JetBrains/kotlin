package test

public trait InheritNullabilityJavaSubtype: Object {

    public trait Super: Object {
        public fun foo(): CharSequence
    }

    public trait Sub: Super {
        override fun foo(): String
    }
}