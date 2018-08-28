//FULL_JDK

class KotlinMap1<K, V> : java.util.AbstractMap<K, V>() {
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = throw UnsupportedOperationException()

    override fun remove(x: K, y: V) = true
}

// method: KotlinMap1::remove
// jvm signature: (Ljava/lang/Object;Ljava/lang/Object;)Z
// generic signature: null

class KotlinMap2 : java.util.AbstractMap<String, Int>() {
    override val entries: MutableSet<MutableMap.MutableEntry<String, Int>>
        get() = throw UnsupportedOperationException()

    override fun remove(x: String, y: Int) = true
}

// method: KotlinMap2::remove
// jvm signature: (Ljava/lang/Object;Ljava/lang/Object;)Z
// generic signature: null

// method: KotlinMap2::remove
// jvm signature: (Ljava/lang/String;I)Z
// generic signature: null
