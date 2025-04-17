// SKIP_JDK6
// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// PARAMETERS_METADATA

// FILE: A.kt

inline class A(val i: Int) {
    fun foo(v: Int) = i + v
}

fun A.bar() = this.i

fun box(): String {
    val method = Class.forName("A").declaredMethods.single { it.name == "foo-impl" }
    val parameters = method.getParameters()
    if (parameters[0].name != "this-i") return "wrong name on receiver parameter: ${parameters[0].name}"
    if (parameters[1].name != "v") return "wrong name on actual parameter: ${parameters[1].name}"

    val extensionMethod = Class.forName("AKt").declaredMethods.single { it.name.startsWith("bar") }
    val extensionMethodParameters = extensionMethod.getParameters()
    if (extensionMethodParameters[0].name != "\$this\$bar-i")
        return "wrong name on extension receiver parameter: ${extensionMethodParameters[0].name}"

    return "OK"
}
