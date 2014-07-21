package test

// See SubclassFromGenericAndNot, as well
public trait Kt3302 {
    public trait BSONObject  {
        public fun put(p0: String, p1: Any): Any?

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait LinkedHashMap<K, V>  {
        public fun put(key: K, value: V): V?

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait BasicBSONObject : LinkedHashMap<String, Any>, BSONObject {
        override fun put(key: String, value: Any): Any?
    }
}
