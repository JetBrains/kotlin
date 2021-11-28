// TARGET_BACKEND: JVM
// WITH_STDLIB
// JVM_TARGET: 1.6

@Target(AnnotationTarget.TYPE)
annotation class A

fun box(): String {
    A::class.java.declaredAnnotations.joinToString()
    ExtensionFunctionType::class.java.declaredAnnotations.joinToString()
    return "OK"
}
