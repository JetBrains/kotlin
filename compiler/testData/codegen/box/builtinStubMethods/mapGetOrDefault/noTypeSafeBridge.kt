// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_RUNTIME

class A : MutableMap<Any, Any?> {
    override val entries: MutableSet<MutableMap.MutableEntry<Any, Any?>>
        get() = throw UnsupportedOperationException()
    override val keys: MutableSet<Any>
        get() = throw UnsupportedOperationException()
    override val values: MutableCollection<Any?>
        get() = throw UnsupportedOperationException()

    override fun clear() {
        throw UnsupportedOperationException()
    }

    override fun put(key: Any, value: Any?): Any? {
        throw UnsupportedOperationException()
    }

    override fun putAll(from: Map<out Any, Any?>) {
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

    override fun containsValue(value: Any?): Boolean {
        throw UnsupportedOperationException()
    }

    override fun get(key: Any): Any? {
        throw UnsupportedOperationException()
    }

    override fun isEmpty(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun getOrDefault(key: Any, defaultValue: Any?): Any? {
        if (key == "abc") return "cde"
        return defaultValue
    }
}

fun box(): String {
    val a = A()
    if (a.getOrDefault("abc", "xyz") != "cde") return "fail 1"
    if (a.getOrDefault("56", "123") != "123") return "fail 2"

    val mm = a as MutableMap<Any?, Any?>
    if (mm.getOrDefault("abc", "xyz") != "cde") return "fail 3"
    if (mm.getOrDefault("56", 123) != 123) return "fail 4"
    if (mm.getOrDefault(1, "456") != "456") return "fail 5"
    if (mm.getOrDefault(null, "qwe") != "qwe") return "fail 6"
    if (mm.getOrDefault("abc", null) != "cde") return "fail 7"

    return "OK"
}
