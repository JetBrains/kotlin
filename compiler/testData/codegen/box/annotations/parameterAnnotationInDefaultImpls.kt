// IGNORE_BACKEND_FIR: JVM_IR
// SKIP_JDK6
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_REFLECT

package test

annotation class Anno(val value: String)

interface Test {
    fun foo(@Anno("OK") a: String) = "123"
}

fun box(): String {
    val testMethod = Class.forName("test.Test\$DefaultImpls").declaredMethods.single()
    //return (::test.parameters.single().annotations.single() as Simple).value
    val receiverAnnotations = (testMethod.parameters[0]).annotations
    if (receiverAnnotations.isNotEmpty()) return "fail: receiver parameter should not have any annotations, but: ${receiverAnnotations.joinToString()}"

    val value2 = ((testMethod.parameters[1]).annotations.single() as Anno).value

    return value2
}
