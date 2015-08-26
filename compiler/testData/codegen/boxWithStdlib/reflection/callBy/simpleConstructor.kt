class A(val result: String = "OK")

fun box(): String = ::A.callBy(mapOf()).result
