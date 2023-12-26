// WITH_STDLIB
// KJS_WITH_FULL_RUNTIME
// JVM_ABI_K1_K2_DIFF: KT-63864

// FILE: 1.kt
package test

class WeakReference<T>(val value: T)

inline fun <K, V> MutableMap<K, WeakReference<V>>.getOrPutWeak(key: K, defaultValue: ()->V): V {
    val value = get(key)?.value
    return if (value == null) {
        val answer = defaultValue()
        put(key, WeakReference(answer))
        answer
    } else {
        value
    }
}


// FILE: 2.kt
import test.*

class LabelHolder {

    fun test(): String {
        return "hello".label
    }

    private val labels = hashMapOf<String?, WeakReference<String>>()

    private val String?.label: String
        get(): String = labels.getOrPutWeak(this) { "OK" }
}

fun box(): String {
    return LabelHolder().test()
}
