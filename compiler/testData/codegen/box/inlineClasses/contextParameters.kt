// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// WORKS_WHEN_VALUE_CLASS
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// PARAMETERS_METADATA


OPTIONAL_JVM_INLINE_ANNOTATION
value class A(val x: Int) {
    context(y: Int)
    fun f(regular: Int) = x + y + regular

    context(y: A)
    fun g(regular: Int) = x + y.x + regular
}

fun box(): String {
    checkInlineClass()

    return "OK"
}

private fun checkInlineClass() {
    val a = A(3)
    val sum1 = with(2) { a.f(100) }
    if (sum1 != 105) error(sum1.toString())
    val sum2 = with(a) { a.g(100) }
    if (sum2 != 106) error(sum2.toString())

    for (methodName in listOf("f-impl", "g-miQMsnA")) {
        checkParameters("A", methodName)
    }
}

private fun checkParameters(className: String, methodName: String) {
    val method = Class.forName(className).declaredMethods.single { it.name == methodName }
    val parameters = method.getParameters()
    for (parameter in parameters) {
        val condition = when (parameter.name) {
            "this-x" -> parameter.isSynthetic() && !parameter.isImplicit()
            "y", "y-x" -> !parameter.isSynthetic() && parameter.isImplicit()
            "regular" -> !parameter.isSynthetic() && !parameter.isImplicit()
            else -> error("Unknown parameter: $parameter")
        }
        require(condition) { "wrong modifier on parameter $parameter of $methodName: ${parameter.modifiers}" }
    }
}
