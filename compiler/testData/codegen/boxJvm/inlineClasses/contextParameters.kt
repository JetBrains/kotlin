// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ContextParameters
// PARAMETERS_METADATA

package test

OPTIONAL_JVM_INLINE_ANNOTATION
value class A(val x: Int) {
    context(y: Int)
    fun f(regular: Int) = x + y + regular

    context(y: A)
    fun g(regular: Int) = this.x + y.x + regular
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

    checkParameters("test.A", "f-impl")
    checkParameters("test.A", "g-3dzUp6c", "g-138r1U4") // Android tests relocate package
}

private fun checkParameters(className: String, vararg expectedMethodNames: String) {
    val methodNames = Class.forName(className).declaredMethods.map { it.name }
    if (expectedMethodNames.none { it in methodNames }) {
        error("${expectedMethodNames.toList()} are not found in $methodNames")
    }
    val method = Class.forName(className).declaredMethods.single { it.name in expectedMethodNames }
    val methodName = methodNames.single { it in expectedMethodNames }
    val parameters = method.getParameters()
    for ((index, parameter) in parameters.withIndex()) {
        val isLast = index == parameters.lastIndex
        if (isLast == parameter.isSynthetic()) {
            error("wrong modifier on parameter $parameter of $methodName: ${parameter.modifiers}")
        }
    }
}
