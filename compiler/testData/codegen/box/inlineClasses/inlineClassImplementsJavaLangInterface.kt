// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class InlineRunnable(val block: () -> Unit) : Runnable {
    override fun run() = block()
}

fun box(): String {
    var result = "fail"
    (InlineRunnable { result = "OK" } as Runnable).run()
    return result
}
