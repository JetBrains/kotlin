fun foo(result: String = "OK") = result

fun box(): String = ::foo.callBy(mapOf())
