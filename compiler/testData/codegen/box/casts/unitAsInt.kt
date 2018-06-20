// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

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
