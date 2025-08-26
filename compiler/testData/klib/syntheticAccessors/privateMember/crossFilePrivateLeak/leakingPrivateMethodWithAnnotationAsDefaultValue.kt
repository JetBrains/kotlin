// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: A.kt
import kotlin.reflect.full.findAnnotation

class A {
    private annotation class Annotation(val s: String = "O")

    @Annotation
    private fun foo(x: Annotation? = A::class.findAnnotation<Annotation>()) = x?.s

    @Annotation(s = "K")
    private fun bar(x: Annotation? = A::class.findAnnotation<Annotation>()) = x?.s

    internal inline fun baz() = foo() + bar()
}

// FILE: B.kt
fun box(): String {
    return A().baz()
}
