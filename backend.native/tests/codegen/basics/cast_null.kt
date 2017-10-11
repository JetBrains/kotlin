package codegen.basics.cast_null

import kotlin.test.*

@Test
fun runTest() {
    testCast(null, false)
    testCastToNullable(null, true)
    testCastToNullable(TestKlass(), true)
    testCastToNullable("", false)
    testCastNotNullableToNullable(TestKlass(), true)
    testCastNotNullableToNullable("", false)

    println("Ok")
}

class TestKlass

fun ensure(b: Boolean) {
    if (!b) {
        println("Error")
    }
}

fun testCast(x: Any?, expectSuccess: Boolean) {
    try {
        x as TestKlass
    } catch (e: Throwable) {
        ensure(!expectSuccess)
        return
    }
    ensure(expectSuccess)
}

fun testCastToNullable(x: Any?, expectSuccess: Boolean) {
    try {
        x as TestKlass?
    } catch (e: Throwable) {
        ensure(!expectSuccess)
        return
    }
    ensure(expectSuccess)
}

fun testCastNotNullableToNullable(x: Any, expectSuccess: Boolean) {
    try {
        x as TestKlass?
    } catch (e: Throwable) {
        ensure(!expectSuccess)
        return
    }
    ensure(expectSuccess)
}