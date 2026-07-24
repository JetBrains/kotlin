// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// JVM_EXPOSE_BOXED

@JvmInline
value class MyInlineClass<E>(val all: List<E>) : List<E> by all

fun box(): String {
    val c: Collection<String> = MyInlineClass(listOf("a"))
    if (c.size != 1) return "FAIL: ${c.size}"
    return "OK"
}

