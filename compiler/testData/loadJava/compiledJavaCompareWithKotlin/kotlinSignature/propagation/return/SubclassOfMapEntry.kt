package test

public trait SubclassOfMapEntry<K, V>: MutableMap.MutableEntry<K, V> {
    override fun setValue(p0: V) : V
}
