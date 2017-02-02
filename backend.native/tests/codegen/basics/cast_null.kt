fun main(args: Array<String>) {
    testCast(null, false)
    testCastToNullable(null, true)
    testCastToNullable(Test(), true)
    testCastToNullable("", false)
    testCastNotNullableToNullable(Test(), true)
    testCastNotNullableToNullable("", false)

    println("Ok")
}

class Test

fun ensure(b: Boolean) {
    if (!b) {
        println("Error")
    }
}

fun testCast(x: Any?, expectSuccess: Boolean) {
    try {
        x as Test
    } catch (e: Throwable) {
        ensure(!expectSuccess)
        return
    }
    ensure(expectSuccess)
}

fun testCastToNullable(x: Any?, expectSuccess: Boolean) {
    try {
        x as Test?
    } catch (e: Throwable) {
        ensure(!expectSuccess)
        return
    }
    ensure(expectSuccess)
}

fun testCastNotNullableToNullable(x: Any, expectSuccess: Boolean) {
    try {
        x as Test?
    } catch (e: Throwable) {
        ensure(!expectSuccess)
        return
    }
    ensure(expectSuccess)
}