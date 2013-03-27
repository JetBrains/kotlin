package test

public trait TwoSuperclassesReturnSameJavaType: Object {

    public trait Super1: Object {
        public fun foo(): CharSequence?

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Super2: Object {
        public fun foo(): CharSequence

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super1, Super2 {
        override fun foo(): CharSequence
    }
}