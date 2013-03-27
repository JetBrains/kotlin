package test

public trait MutableToReadOnly: Object {

    public trait Super: Object {
        public fun foo(p0: MutableList<String>)

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super {
        override fun foo(p0: MutableList<String>)
    }
}