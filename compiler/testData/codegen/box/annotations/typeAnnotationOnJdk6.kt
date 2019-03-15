// TARGET_BACKEND: JVM
// WITH_RUNTIME

@Target(AnnotationTarget.TYPE)
annotation class A

fun box(): String {
    A::class.java.declaredAnnotations.joinToString()
    ExtensionFunctionType::class.java.declaredAnnotations.joinToString()
    return "OK"
}
