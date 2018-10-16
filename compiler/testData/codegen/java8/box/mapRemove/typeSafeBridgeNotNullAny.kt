// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_RUNTIME

class A : MutableMap<Any, Any> {
    override val entries: MutableSet<MutableMap.MutableEntry<Any, Any>>
        get() = throw UnsupportedOperationException()
    override val keys: MutableSet<Any>
        get() = throw UnsupportedOperationException()
    override val values: MutableCollection<Any>
        get() = throw UnsupportedOperationException()

    override fun clear() {
        throw UnsupportedOperationException()
    }

    override fun put(key: Any, value: Any): Any? {
        throw UnsupportedOperationException()
    }

    override fun putAll(from: Map<out Any, Any>) {
        throw UnsupportedOperationException()
    }

    override fun remove(key: Any): Any? {
        throw UnsupportedOperationException()
    }

    override val size: Int
        get() = throw UnsupportedOperationException()

    override fun containsKey(key: Any): Boolean {
        throw UnsupportedOperationException()
    }

    override fun containsValue(value: Any): Boolean {
        throw UnsupportedOperationException()
    }

    override fun get(key: Any): Any? {
        throw UnsupportedOperationException()
    }

    override fun isEmpty(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun remove(key: Any, value: Any): Boolean {
        val h = key.hashCode() + value.hashCode()
        if (h != ("abc".hashCode() + "cde".hashCode())) return false
        return key == "abc" && value == "cde"
    }
}

fun box(): String {
    val a = A()
    if (!a.remove("abc", "cde")) return "fail 1"
    if (a.remove("abc", "123")) return "fail 2"

    val mm = a as MutableMap<Any?, Any?>
    if (!mm.remove("abc", "cde")) return "fail 3"
    if (mm.remove("abc", "123")) return "fail 4"
    if (mm.remove(1, "cde")) return "fail 5"
    if (mm.remove(null, "cde")) return "fail 6"
    if (mm.remove("abc", null)) return "fail 7"
    if (mm.remove(null, null)) return "fail 8"

    return "OK"
}
