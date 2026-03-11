// TARGET_BACKEND: JVM
// WITH_STDLIB

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
