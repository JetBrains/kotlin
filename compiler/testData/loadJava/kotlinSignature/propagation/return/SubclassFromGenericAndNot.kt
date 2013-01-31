package test

public trait SubclassFromGenericAndNot: Object {

    public trait NonGeneric : Object {
        public fun foo(): String?
    }

    public trait Generic<T> : Object {
        public fun foo(): T
    }

    public trait Sub : NonGeneric, Generic<String> {
        override fun foo(): String
    }
}
