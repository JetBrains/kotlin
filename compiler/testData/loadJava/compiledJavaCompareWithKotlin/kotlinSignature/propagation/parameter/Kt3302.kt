package test

// See SubclassFromGenericAndNot, as well
public trait Kt3302: Object {
    public trait BSONObject : Object {
        public fun put(p0: String, p1: Any): Any?

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait LinkedHashMap<K, V> : Object {
        public fun put(p0: K, p1: V): V?

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait BasicBSONObject : LinkedHashMap<String, Any>, BSONObject {
        override fun put(p0: String, p1: Any): Any?
    }
}
