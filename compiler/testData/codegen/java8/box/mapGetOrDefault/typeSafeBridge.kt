// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_RUNTIME

class A : MutableMap<String, String> {
    override val entries: MutableSet<MutableMap.MutableEntry<String, String>>
        get() = throw UnsupportedOperationException()
    override val keys: MutableSet<String>
        get() = throw UnsupportedOperationException()
    override val values: MutableCollection<String>
        get() = throw UnsupportedOperationException()

    override fun clear() {
        throw UnsupportedOperationException()
    }

    override fun put(key: String, value: String): String? {
        throw UnsupportedOperationException()
    }

    override fun putAll(from: Map<out String, String>) {
        throw UnsupportedOperationException()
    }

    override fun remove(key: String): String? {
        throw UnsupportedOperationException()
    }

    override val size: Int
        get() = throw UnsupportedOperationException()

    override fun containsKey(key: String): Boolean {
        throw UnsupportedOperationException()
    }

    override fun containsValue(value: String): Boolean {
        throw UnsupportedOperationException()
    }

    override fun get(key: String): String? {
        throw UnsupportedOperationException()
    }

    override fun isEmpty(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun getOrDefault(key: String, defaultValue: String): String {
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
    if (mm.getOrDefault("56", "123") != "123") return "fail 4"
    if (mm.getOrDefault(1, "456") != "456") return "fail 5"
    if (mm.getOrDefault(null, "qwe") != "qwe") return "fail 6"

    try {
        // This is a known problem, there's no way to implement type-safe bridge/barrier properly:
        // 'override fun getOrDefault(key: String, defaultValue: String): String' expects two strings,
        // and returning defaultValue if Int was received seems incorrect here
        mm.getOrDefault("abc", 123)
        return "fail 7"
    } catch (e: java.lang.ClassCastException) {
    }

    return "OK"
}
