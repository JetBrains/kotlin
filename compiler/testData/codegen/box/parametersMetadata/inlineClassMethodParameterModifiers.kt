// SKIP_JDK6
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// PARAMETERS_METADATA

// FILE: A.kt

inline class A(val i: Int) {
    fun f() = i
}

fun A.extension() = this.i

fun box(): String {
    val method = Class.forName("A").declaredMethods.single { it.name == "f-impl" }
    val parameters = method.getParameters()
    if (!parameters[0].isSynthetic()) return "wrong modifier on receiver parameter: ${parameters[0].modifiers}"

    val extensionMethod = Class.forName("AKt").declaredMethods.single { it.name.contains("extension") }
    val extensionMethodParameters = extensionMethod.getParameters()
    if (extensionMethodParameters[0].isSynthetic() || extensionMethodParameters[0].isImplicit())
        return "wrong modifier on extension receiver parameter: ${extensionMethodParameters[0].modifiers}"

    return "OK"
}
