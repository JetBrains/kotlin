// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

fun box() = if(arrayOfNulls<Int>(10).isArrayOf<java.lang.Integer>()) "OK" else "fail"
