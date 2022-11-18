// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +CustomEqualsInInlineClasses
// TARGET_BACKEND: JVM_IR

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC1(val x: String)

lateinit var a: IC1

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC2<T : Number>(val x: T)

class MyClass {
    lateinit var b: IC2<Int>

    fun isInit() = ::b.isInitialized
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC3<T>(val x: Comparable<T>)

inline fun <reified T> assertThrows(block: () -> Unit): Boolean {
    try {
        block.invoke()
    } catch (t: Throwable) {
        return t is T
    }
    return false
}

fun box(): String {
    if (::a.isInitialized) return "Fail 1.1"
    if (!assertThrows<UninitializedPropertyAccessException> { println(a) }) return "Fail 1.2"
    a = IC1("a")
    if (!::a.isInitialized) return "Fail 1.3"
    if (a.x != "a") return "Fail 1.4"

    val inst = MyClass()
    if (inst.isInit()) return "Fail 2.1"
    if (!assertThrows<UninitializedPropertyAccessException> { println(inst.b) }) return "Fail 2.2"
    inst.b = IC2(42)
    if (!inst.isInit()) return "Fail 2.3"
    if (inst.b.x != 42) return "Fail 2.4"

    lateinit var z: IC3<Double>
    if (!assertThrows<UninitializedPropertyAccessException> { println(z) }) return "Fail 3.1"
    z = IC3(5.0)
    if (z.x != 5.0) return "Fail 3.2"

    return "OK"
}