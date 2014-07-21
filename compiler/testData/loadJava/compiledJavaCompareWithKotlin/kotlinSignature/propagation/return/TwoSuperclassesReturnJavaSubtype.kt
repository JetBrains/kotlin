package test

public trait TwoSuperclassesReturnJavaSubtype {

    public trait Super1 {
        public fun foo(): CharSequence?

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Super2 {
        public fun foo(): CharSequence

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super1, Super2 {
        override fun foo(): String
    }
}
