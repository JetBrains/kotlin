// TARGET_BACKEND: JVM
// WITH_RUNTIME


@JvmName("fooA")
private fun String.foo(t: String?): String = this

private fun String.foo(t: String): String = this

fun runNoInline(fn: () -> String) = fn()

fun box() =
        runNoInline { "O".foo("") } +
        runNoInline { "K".foo(null) }

