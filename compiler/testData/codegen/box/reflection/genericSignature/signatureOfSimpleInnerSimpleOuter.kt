// IGNORE_BACKEND_FIR: JVM_IR
// SKIP_JDK6
// TARGET_BACKEND: JVM
// WITH_REFLECT

abstract class Outer {
    inner class Inner
    fun foo(): Inner? = null
}

fun box(): String {
    kotlin.test.assertEquals(
            "class Outer\$Inner",
            Outer::class.java.declaredMethods.single().genericReturnType.toString())

    return "OK"
}