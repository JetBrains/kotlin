// WITH_RUNTIME

open class A {
    private val tags = mutableListOf("a", "b")
        public get(): List<String>

    fun registerTag(tag: String) {
        tags.add(tag)
    }
}

fun box(): String {
    val a = A()

    if (a.tags.firstOrNull() != "a") {
        return "fail: 1 => ${a.tags}"
    }

    val tags = a.tags

    a.registerTag("c")

    if (tags.lastOrNull() != "c") {
        return "fail: 1 => ${a.tags}"
    }

    return "OK"
}