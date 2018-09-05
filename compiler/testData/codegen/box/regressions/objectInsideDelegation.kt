// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

val b: First by lazy {
    object : First {   }
}

private val withoutType by lazy {
    object : First { }
}

private val withTwoSupertypes by lazy {
    object : First, Second { }
}

interface First
interface Second

fun box(): String {
    b
    withoutType
    withTwoSupertypes
    return "OK"
}