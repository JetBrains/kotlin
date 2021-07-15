// WITH_RUNTIME

interface I {
    val tags: List<String>
}

open class A : I {
    private override val tags = mutableListOf<String>()
        public get(): List<String>

    fun registerTag(tag: String) {
        tags.add(tag)
    }
}

fun box(): String {
    val a = A()
    val tags = a.tags
    a.registerTag("#test")

    return if (
        a.tags.firstOrNull() == "#test" &&
        tags == a.tags
    ) {
        "OK"
    } else {
        "fail"
    }
}