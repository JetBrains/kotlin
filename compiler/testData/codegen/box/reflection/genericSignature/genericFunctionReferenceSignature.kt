// TARGET_BACKEND: JVM
// TARGET_BACKEND: NATIVE

// WITH_REFLECT

package test

fun <T> foo(x: T) = x

fun box(): String {
    val bar: kotlin.reflect.KFunction1<Int, Int> = ::foo
    val returnType = bar.returnType
    if (returnType.toString() != "T") return returnType.toString()
    return "OK"
}
