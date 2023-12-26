// !JVM_DEFAULT_MODE: all
// SKIP_JDK6
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
// FULL_JDK
// JVM_ABI_K1_K2_DIFF: KT-57301

// There should be no DefaultImpls method for MutableMap.remove(K;V)
interface A<K, V> : MutableMap<K, V>

class B : A<String, String>, java.util.AbstractMap<String, String>() {
    override val entries: MutableSet<MutableMap.MutableEntry<String, String>>
        get() = java.util.HashSet()
}

interface C<K, V> : MutableMap<K, V> {
    override fun remove(key: K, value: V) = true
}

class D : A<String, String>, java.util.AbstractMap<String, String>() {
    override val entries: MutableSet<MutableMap.MutableEntry<String, String>>
        get() = java.util.HashSet()
}

fun box(): String {
    val x1 = B().remove("1", "2")
    if (x1) return "fail 1"

    val x2 = D().remove("3", "4")
    if (x1) return "fail 2"

    return "OK"
}
