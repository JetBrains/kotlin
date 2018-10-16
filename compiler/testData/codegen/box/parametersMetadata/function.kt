// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FULL_JDK
// KOTLIN_CONFIGURATION_FLAGS: +JVM.PARAMETERS_METADATA

class A() {
    fun test(OK: String) {

    }
}

fun box(): String {
    val clazz = A::class.java
    val method = clazz.getDeclaredMethod("test", String::class.java)
    val parameters = method.getParameters()

    if (parameters[0].modifiers != 0) return "wrong modifier on value parameter: ${parameters[0].modifiers}"
    return parameters[0].name
}