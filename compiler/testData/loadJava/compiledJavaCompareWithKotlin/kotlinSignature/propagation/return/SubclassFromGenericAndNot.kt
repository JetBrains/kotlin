package test

public trait SubclassFromGenericAndNot: Object {

    public trait NonGeneric : Object {
        public fun foo(): String?

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Generic<T> : Object {
        public fun foo(): T

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub : NonGeneric, Generic<String> {
        override fun foo(): String
    }
}
