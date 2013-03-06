package test

public trait TwoSuperclassesReturnSameJavaType: Object {

    public trait Super1: Object {
        public fun foo(): CharSequence?
    }

    public trait Super2: Object {
        public fun foo(): CharSequence
    }

    public trait Sub: Super1, Super2 {
        override fun foo(): CharSequence
    }
}