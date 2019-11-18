// IGNORE_BACKEND_FIR: JVM_IR
// SKIP_JDK6
// TARGET_BACKEND: JVM
// WITH_REFLECT

abstract class Outer<S> {
    inner class Inner<R>
    fun <R> foo(): Inner<R>? = null
}

fun box(): String {
    kotlin.test.assertEquals(
        "Outer<S>\$Inner<R>",
        Outer::class.java.declaredMethods.single().genericReturnType.toString()
    )

    return "OK"
}