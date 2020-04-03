// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR

import java.util.AbstractMap

// Neither of getSize, getKeys, getEntries, getValues generated here should be final.
abstract class AbstractMutableMap<K, V> : MutableMap<K, V>, AbstractMap<K, V>()

class MyMap<K, V> : AbstractMutableMap<K, V>() {
    override val size: Int
        get() = 1
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = null!!
}

fun box(): String =
    if (MyMap<String, String>().size == 1) "OK" else "Fail"
