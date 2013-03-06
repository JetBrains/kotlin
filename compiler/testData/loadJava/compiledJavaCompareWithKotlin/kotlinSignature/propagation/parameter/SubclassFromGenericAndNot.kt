package test

// Extracted from KT-3302, see Kt3302 test, as well
public trait SubclassFromGenericAndNot: Object {

    public trait NonGeneric : Object {
        public fun foo(p0: String)
    }

    public trait Generic<T> : Object {
        public fun foo(p0: T)
    }

    public trait Sub : NonGeneric, Generic<String> {
        override fun foo(p0: String)
    }
}
