// WITH_STDLIB

import kotlin.coroutines.*

class SuspendingMutableMap<K : Any, V : Any>(
    private val map: MutableMap<K, V>,
) : Map<K, V> {
    suspend fun clear() {
        map.clear()
    }

    override val entries: Set<Map.Entry<K, V>>
        get() = TODO("Not yet implemented")
    override val keys: Set<K>
        get() = TODO("Not yet implemented")
    override val size: Int
        get() = TODO("Not yet implemented")
    override val values: Collection<V>
        get() = TODO("Not yet implemented")

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun get(key: K): V? {
        TODO("Not yet implemented")
    }

    override fun containsValue(value: V): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsKey(key: K): Boolean {
        TODO("Not yet implemented")
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

fun box(): String {
    builder {
        val m = mutableMapOf(1 to 1)
        val map = SuspendingMutableMap(m)
        map.clear()
        if (m.isNotEmpty()) error ("FAIL")
    }
    return "OK"
}