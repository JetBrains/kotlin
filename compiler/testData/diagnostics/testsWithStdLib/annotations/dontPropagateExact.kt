// FIR_IDENTICAL
// FULL_JDK
// SKIP_TXT
package test
import kotlin.reflect.KClass

annotation class RunsInActiveStoreMode

val w1 = ""::class.java
val w2 = ""::class.java

private fun <T : Annotation> foo(annotationClass: Class<T>) = w1.getAnnotation(annotationClass) ?: w2.getAnnotation(annotationClass)

fun main() {
    val x: Any = foo(RunsInActiveStoreMode::class.java)
}
