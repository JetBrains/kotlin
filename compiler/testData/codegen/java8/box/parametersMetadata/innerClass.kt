// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FULL_JDK
// KOTLIN_CONFIGURATION_FLAGS: +JVM.PARAMETERS_METADATA

class A {
    inner class B
}

fun box(): String {
    val clazz = A.B::class.java
    val constructor = clazz.getDeclaredConstructors().single()
    val parameters = constructor.getParameters()

    if (parameters[0].name != "this$0") return "wrong outer name: ${parameters[0].name}"
    if (!parameters[0].isImplicit() || parameters[0].isSynthetic()) return "wrong outer flags: ${parameters[0].modifiers}"

    return "OK"
}