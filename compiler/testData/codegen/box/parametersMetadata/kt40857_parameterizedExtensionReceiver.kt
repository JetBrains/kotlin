// SKIP_JDK6
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// PARAMETERS_METADATA

import java.lang.reflect.ParameterizedType

fun List<String>.bar(i: Int) = Unit

fun box(): String {
    val function = object {}.javaClass.enclosingClass.getDeclaredMethods().single { it.name == "bar" }
    val type = function.parameters[0].parameterizedType
    if (type !is ParameterizedType || type.toString() != "java.util.List<java.lang.String>") return "Fail: $type ${type.javaClass}"
    return "OK"
}
