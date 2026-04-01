// WITH_STDLIB
import kotlin.test.*

fun box(): String {
    testCast(null, false)
    testCastToNullable(null, true)
    testCastToNullable(TestKlass(), true)
    testCastToNullable("", false)
    testCastNotNullableToNullable(TestKlass(), true)
    testCastNotNullableToNullable("", false)

    return "OK"
}

class TestKlass

fun ensure(b: Boolean) {
    if (!b) {
        throw Error("Error")
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
