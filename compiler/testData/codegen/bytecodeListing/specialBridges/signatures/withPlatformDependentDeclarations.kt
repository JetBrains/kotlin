// FULL_JDK
// JVM_TARGET: 1.8
// WITH_SIGNATURES
interface I

abstract class T1<K, V> : MutableMap<K, V> {
    // This declaration should have a generic signature (Ljava/lang/Object;TV;)TV; to match the declaration
    // in java.util.Map
    override fun getOrDefault(key: K, value: V): V = value

    // This declaration does not override the getOrDefault method in java.util.Map and should have a
    // normal generic signature of (TK;I)I
    fun getOrDefault(key: K, value: Int): Int = value + 1

    // This declaration overrides a non-generic method in java.util.Map and should not have a generic signature
    override fun remove(key: K, value: V): Boolean = false

    // This declaration does not override the remove method in java.util.Map and should have a normal
    // generic signature of (TK;I)Z
    fun remove(key: K, value: Int): Boolean = true
}

abstract class T2<V> : MutableMap<String, V> {
    // This declaration does not override the corresponding method in java.util.Map and should
    // have a normal generic signature of
    //
    //   (Ljava/lang/String;TV;)TV;
    //
    // However, we also generate an additional bridge method with signature
    //
    //   (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //
    // to override the corresponding method in java.util.Map. If we generate a generic signature at
    // all - it's not really checked for bridge methods - it should be
    //
    //   (Ljava/lang/Object;TV;)TV;
    //
    override fun getOrDefault(key: String, value: V): V = value

    // This does not override the getOrDefault method in java.util.Map and should have a
    // normal generic signature of (Ljava/lang/Integer;TV;)TV;
    fun getOrDefault(key: Int?, value: V): V = value

    // This declaration does not override the corresponding method in java.util.Map and should
    // have a normal generic signature of
    //
    //   (Ljava/lang/String;TV;)Z
    //
    // However, we also generate an additional non-generic bridge method
    override fun remove(key: String, value: V): Boolean = false

    // This declaration does not override the remove method in java.util.Map and should have a normal
    // generic signature of (Ljava/lang/Integer;TV;)Z
    fun remove(key: Int?, value: V): Boolean = true
}

abstract class T3<K, V : I> : MutableMap<K, V> {
    // This declaration overrides the corresponding declaration in java.util.Map and should have
    // the following modified generic signature.
    //
    //   (Ljava/lang/Object;TV;)TV;
    //
    // The additional bridge method should have no generic signature or the same one.
    override fun getOrDefault(key: K, value: V): V = value

    // This declaration overrides nothing and should have the following generic signature.
    //
    //   (TK;I)LI;
    //
    fun getOrDefault(key: K, value: Int): I? = null

    // This declaration does not override the corresponding declaration in java.util.Map
    // and should have the following generic signature.
    //
    //   (TK;TV;)Z
    //
    // The corresponding bridge overrides a non-generic method in java.util.Map and should
    // not have a generic signature.
    override fun remove(key: K, value: V): Boolean = false

    // This declaration overrides nothing and should have the following generic signature.
    //
    //   (TK;I)Z
    //
    fun remove(key: K, value: Int): Boolean = true
}

abstract class T4<K, V> : Map<K, V> {
    // This declaration implicitly overrides a non-generic method in java.util.Map and
    // should not have a generic signature.
    fun remove(key: K, value: V): Boolean = false

    // The behavior for the remaining declarations is the same as in T1
    // (Ljava/lang/Object;TV;)TV;
    override fun getOrDefault(key: K, value: V): V = value
    // (TK;I)I
    fun getOrDefault(key: K, value: Int): Int = value + 1
    // (TK;I)Z
    fun remove(key: K, value: Int): Boolean = true
}
