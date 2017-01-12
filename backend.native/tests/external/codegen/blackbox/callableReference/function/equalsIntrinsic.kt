// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

class A

fun box() = if ((A::equals)(A(), A())) "Fail" else "OK"
