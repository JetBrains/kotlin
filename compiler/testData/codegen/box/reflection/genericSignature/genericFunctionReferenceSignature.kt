// WITH_REFLECT

package test

fun <T> foo(x: T) = x

fun box(): String {
    val bar: Function1<Int, Int> = ::foo
    val returnType = (bar as kotlin.reflect.KFunction<*>).returnType
    if (returnType.toString() != "T") return returnType.toString()
    return "OK"
}
