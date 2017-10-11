package serialization.deserialized_inline0

import kotlin.test.*


fun inline_todo() {
    try {
        TODO("OK")
    } catch (e: Throwable) {
        println(e.message)
    }
}

fun inline_maxof() {
    println(maxOf(10, 17))
    println(maxOf(17, 13))
    println(maxOf(17, 17))
}

fun inline_assert() {
    //assert(true)
}

fun inline_areEqual() {
    val a = 17
    val b = "some string"
    println(konan.internal.areEqual(a, 17))
    println(konan.internal.areEqual(a, a))
    println(konan.internal.areEqual(17, 17))
    println(konan.internal.areEqual(b, "some string"))
    println(konan.internal.areEqual("some string", b))
    println(konan.internal.areEqual(b, b))
}

@Test fun runTest() {
    inline_todo()
    inline_assert()
    inline_maxof()
    inline_areEqual()
}

