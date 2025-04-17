// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// WORKS_WHEN_VALUE_CLASS
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters, +ValueClasses
// PARAMETERS_METADATA

OPTIONAL_JVM_INLINE_ANNOTATION
value class B(val x: Int, val y: Int) {
    context(z: Int)
    fun f(regular: Int) = x + y + z + regular

    context(z: B)
    fun g(regular: Int) = x + y + z.x + z.y + regular
}

fun box(): String {
    checkMultiFieldValueClass()

    return "OK"
}

private fun checkMultiFieldValueClass() {
    val b = B(3, 4)
    val sum1 = with(2) { b.f(100) }
    if (sum1 != 109) error(sum1.toString())
    val sum2 = with(b) { b.g(100) }
    if (sum2 != 114) error(sum2.toString())

    for (methodName in listOf("f-impl", "g-vc4tCeU")) {
        checkParameters("B", methodName)
    }
}

private fun checkParameters(className: String, methodName: String) {
    val method = Class.forName(className).declaredMethods.single { it.name == methodName }
    val parameters = method.getParameters()
    for (parameter in parameters) {
        val condition = when (parameter.name) {
            "\$v\$c\$B\$-this\$0", "\$v\$c\$B\$-this\$1" -> parameter.isSynthetic() && !parameter.isImplicit()
            "z", "\$v\$c\$B\$-z\$0", "\$v\$c\$B\$-z\$1" -> !parameter.isSynthetic() && parameter.isImplicit()
            "regular" -> !parameter.isSynthetic() && !parameter.isImplicit()
            else -> error("Unknown parameter: $parameter")
        }
        require(condition) { "wrong modifier on parameter $parameter of $methodName: ${parameter.modifiers}" }
    }
}
