// WITH_REFLECT
// IGNORE_BACKEND: JS, NATIVE

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