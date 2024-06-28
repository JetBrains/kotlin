// TARGET_BACKEND: JVM
// WITH_REFLECT

class A(val result: String = "OK")

fun box(): String = ::A.callBy(mapOf()).result
