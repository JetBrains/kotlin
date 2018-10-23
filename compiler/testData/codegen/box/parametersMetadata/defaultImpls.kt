// SKIP_JDK6
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FULL_JDK
// KOTLIN_CONFIGURATION_FLAGS: +JVM.PARAMETERS_METADATA

interface Test {
    fun test(OK: String) = "123"
}


fun box(): String {
    val testMethod = Class.forName("Test\$DefaultImpls").declaredMethods.single()
    val parameters = testMethod.getParameters()

    if (!parameters[0].isSynthetic()) return "wrong modifier on receiver parameter: ${parameters[0].modifiers}"

    if (parameters[1].modifiers != 0) return "wrong modifier on value parameter: ${parameters[1].modifiers}"

    return parameters[1].name
}