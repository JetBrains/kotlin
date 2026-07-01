import kotlin.reflect.full.declaredMembers
import kotlin.jvm.internal.CallableReference

private fun checkFunctionClass(value: Any) {
    val className = value::class.java.simpleName
    if (System.getProperty("kotlin.reflect.jvm.useNewImplementationForJavaInstanceMethods") == "true") {
        check(className == "JavaKNamedFunction") { className }
    } else {
        check(className == "DescriptorKFunction") { className }
    }
}

private fun checkFunctions() {
    val reference = J::f
    reference.toString() // Force computation of `CallableReference.reflected`
    val reflected = CallableReference::class.java.getDeclaredField("reflected").apply { isAccessible = true }.get(reference)
    checkFunctionClass(reflected)

    val fromMembers = J::class.declaredMembers.single { it.name == "f" }
    checkFunctionClass(fromMembers)
}

private fun checkPropertyClass(value: Any) {
    val className = value::class.java.simpleName
    // The flag should not affect Java static properties, since they're already released.
    check(className == "JavaKMutableProperty0") { className }
}

private fun checkProperties() {
    val reference = J::p
    reference.toString() // Force computation of `CallableReference.reflected`
    val reflected = CallableReference::class.java.getDeclaredField("reflected").apply { isAccessible = true }.get(reference)
    val unlazied = Class.forName("kotlin.reflect.jvm.internal.LazyKProperty").getDeclaredMethod("getDelegate").invoke(reflected)
    checkPropertyClass(unlazied)

    val fromMembers = J::class.declaredMembers.single { it.name == "p" }
    checkPropertyClass(fromMembers)
}

fun main() {
    checkFunctions()
    checkProperties()
}
