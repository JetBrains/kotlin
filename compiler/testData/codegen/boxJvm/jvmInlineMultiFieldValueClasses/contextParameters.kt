// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ContextParameters, +JvmInlineMultiFieldValueClasses
// PARAMETERS_METADATA

package test

OPTIONAL_JVM_INLINE_ANNOTATION
value class B(val x: Int, val y: Int) {
    context(z: Int)
    fun f(regular: Int) = x + y + z + regular

    context(z: B)
    fun g(regular: Int) = this.x + this.y + z.x + z.y + regular
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

    checkParameters("test.B", "f-impl")
    checkParameters("test.B", "g-t7jd6Lo", "g-SNIjomc") // Android tests relocate package
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
