// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

@JvmInline
value class MyInlineClass(val all: List<String>) : List<String> by all {
    override fun get(index: Int): String = all[index]
}

fun box(): String {
    val c: List<String> = MyInlineClass(listOf("a"))
    if (c[0] != "a") return "FAIL: ${c[0]}"
    return "OK"
}

