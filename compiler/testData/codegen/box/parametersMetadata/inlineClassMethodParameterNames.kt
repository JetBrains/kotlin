// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR
// SKIP_JDK6
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FULL_JDK
// KOTLIN_CONFIGURATION_FLAGS: +JVM.PARAMETERS_METADATA

// FILE: A.kt

inline class A(val i: Int) {
    fun f() = i
}

fun A.extension() = this.i

fun box(): String {
    val method = Class.forName("A").declaredMethods.single { it.name == "f-impl" }
    val parameters = method.getParameters()
    if (parameters[0].name != "arg0") return "wrong name on receiver parameter: ${parameters[0].name}"

    val extensionMethod = Class.forName("AKt").declaredMethods.single { it.name.contains("extension") }
    val extensionMethodParameters = extensionMethod.getParameters()
    if (extensionMethodParameters[0].name != "\$this\$extension")
        return "wrong name on extension receiver parameter: ${extensionMethodParameters[0].name}"

    return "OK"
}