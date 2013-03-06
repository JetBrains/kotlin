package test

public trait ReadOnlyToMutable: Object {

    public trait Super: Object {
        public fun foo(p0: List<String>)
    }

    public trait Sub: Super {
        override fun foo(p0: List<String>)
    }
}