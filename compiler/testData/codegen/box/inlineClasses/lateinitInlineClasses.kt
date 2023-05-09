// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +InlineLateinit

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

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

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC4(val x: IC1)

lateinit var c: IC4

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
    if (!assertThrows<UninitializedPropertyAccessException> { val t = a }) return "Fail 1.2"
    a = IC1("a")
    if (!::a.isInitialized) return "Fail 1.3"
    if (a.x != "a") return "Fail 1.4"

    val inst = MyClass()
    if (inst.isInit()) return "Fail 2.1"
    if (!assertThrows<UninitializedPropertyAccessException> { val t = inst.b }) return "Fail 2.2"
    inst.b = IC2(42)
    if (!inst.isInit()) return "Fail 2.3"
    if (inst.b.x != 42) return "Fail 2.4"

    lateinit var z: IC3<Double>
    if (!assertThrows<UninitializedPropertyAccessException> { val t = z }) return "Fail 3.1"
    z = IC3(5.0)
    if (z.x != 5.0) return "Fail 3.2"

    if (::c.isInitialized) return "Fail 4.1"
    if (!assertThrows<UninitializedPropertyAccessException> { val t = c }) return "Fail 4.2"
    c = IC4(IC1("c"))
    if (!::c.isInitialized) return "Fail 4.3"
    if (c.x.x != "c") return "Fail 4.4"

    return "OK"
}

//CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 INVOKESTATIC IC1.box-impl
// 0 INVOKESTATIC IC2.box-impl
// 0 INVOKESTATIC IC3.box-impl
// 0 INVOKESTATIC IC4.box-impl