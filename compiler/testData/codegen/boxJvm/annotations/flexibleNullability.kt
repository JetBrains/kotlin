// TARGET_BACKEND: JVM
// WITH_REFLECT
// FULL_JDK

import java.util.Optional

fun optionalValue() = Optional.of("Hello")

fun box(): String {
    val optionalString = ::optionalValue.returnType
    if (optionalString.toString() != "java.util.Optional<kotlin.String>") return "Fail 1"
    if (optionalString.arguments.single().type!!.annotations.size != 0) return "Fail 2"
    return "OK"
}
