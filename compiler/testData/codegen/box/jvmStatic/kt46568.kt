// TARGET_BACKEND: JVM
// WITH_RUNTIME

abstract class Foo {
    companion object {
        @JvmStatic
        fun bar(): Nothing = TODO()
    }
}

fun box(): String {
    try {
        Foo.bar()
    } catch (e: Throwable) {
        return "OK"
    }
    return "FAIL"
}
