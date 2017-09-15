// TARGET_BACKEND: JVM
// LANGUAGE_VERSION: 1.2
// WITH_RUNTIME

class Foo {
    lateinit var bar: String

    fun test(): String {
        var state = 0
        if (run { state++; this }::bar.isInitialized) return "Fail 1"

        bar = "A"
        if (!run { state++; this }::bar.isInitialized) return "Fail 3"

        return if (state == 2) "OK" else "Fail: state=$state"
    }
}

fun box(): String {
    return Foo().test()
}
