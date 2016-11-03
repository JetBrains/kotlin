// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_REFLECT

fun foo(result: String = "OK") = result

fun box(): String = ::foo.callBy(mapOf())
