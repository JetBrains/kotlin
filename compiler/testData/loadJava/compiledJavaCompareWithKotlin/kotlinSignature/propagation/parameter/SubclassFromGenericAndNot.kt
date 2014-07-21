package test

// Extracted from KT-3302, see Kt3302 test, as well
public trait SubclassFromGenericAndNot {

    public trait NonGeneric  {
        public fun foo(p0: String)

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Generic<T>  {
        public fun foo(key: T)

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub : NonGeneric, Generic<String> {
        override fun foo(key: String)
    }
}
