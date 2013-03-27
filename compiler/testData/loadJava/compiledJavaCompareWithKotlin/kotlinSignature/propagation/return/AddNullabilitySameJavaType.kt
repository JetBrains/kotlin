package test

public trait AddNullabilitySameJavaType: Object {

    public trait Super: Object {
        public fun foo(): CharSequence

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super {
        override fun foo(): CharSequence
    }
}