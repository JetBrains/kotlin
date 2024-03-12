// FULL_JDK
// SKIP_TXT
package test
import kotlin.reflect.KClass

annotation class RunsInActiveStoreMode

val w1 = ""::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!>
val w2 = ""::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!>

private fun <T : Annotation> foo(annotationClass: Class<T>) = w1.getAnnotation(annotationClass) ?: w2.getAnnotation(annotationClass)

fun main() {
    val x: Any = foo(RunsInActiveStoreMode::class.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>java<!>)
}
