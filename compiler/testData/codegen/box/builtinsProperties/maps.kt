class A : Map<String, String> {
    override val size: Int get() = 56

    override val isEmpty: Boolean get() {
        throw UnsupportedOperationException()
    }

    override fun containsKey(key: Any?): Boolean {
        throw UnsupportedOperationException()
    }

    override fun containsValue(value: Any?): Boolean {
        throw UnsupportedOperationException()
    }

    override fun get(key: Any?): String? {
        throw UnsupportedOperationException()
    }

    override fun keySet(): Set<String> {
        throw UnsupportedOperationException()
    }

    override fun values(): Collection<String> {
        throw UnsupportedOperationException()
    }

    override fun entrySet(): Set<Map.Entry<String, String>> {
        throw UnsupportedOperationException()
    }
}

fun box(): String {
    val a = A()
    if (a.size != 56) return "fail 1: ${a.size}"

    val x: Map<String, String> = a
    if (x.size != 56) return "fail 2: ${x.size}"

    return "OK"
}