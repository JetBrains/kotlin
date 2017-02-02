fun main(args: Array<String>) {
    testCast<Test>(Test(), true)
    testCast<Test>(null, false)
    testCastToNullable<Test>(null, true)

    println("Ok")
}

class Test

fun ensure(b: Boolean) {
    if (!b) {
        println("Error")
    }
}

fun <T : Any> testCast(x: Any?, expectSuccess: Boolean) {
    try {
        x as T
    } catch (e: Throwable) {
        ensure(!expectSuccess)
        return
    }
    ensure(expectSuccess)
}

fun <T : Any> testCastToNullable(x: Any?, expectSuccess: Boolean) {
    try {
        x as T?
    } catch (e: Throwable) {
        ensure(!expectSuccess)
        return
    }
    ensure(expectSuccess)
}