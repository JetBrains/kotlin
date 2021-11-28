// TARGET_BACKEND: JVM

// WITH_STDLIB

fun foo() {}

fun box(): String {
    try {
        foo() as Int?
    }
    catch (e: ClassCastException) {
        return "OK"
    }
    catch (e: Throwable) {
        return "Fail: ClassCastException should have been thrown, but was instead ${e.javaClass.getName()}: ${e.message}"
    }

    return "Fail: no exception was thrown"
}
