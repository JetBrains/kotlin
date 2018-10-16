// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FULL_JDK
// KOTLIN_CONFIGURATION_FLAGS: +JVM.PARAMETERS_METADATA

open class A(val s: String)

fun test(OK: String) = object : A(OK) {
}

fun box(): String {
    val value = test("OK")
    val clazz = value.javaClass
    val constructor = clazz.getDeclaredConstructors().single()
    val parameters = constructor.getParameters()

    if (!parameters[0].isSynthetic()  || parameters[0].isImplicit()) return "wrong modifier on value parameter: ${parameters[0].modifiers}"
    return value.s
}