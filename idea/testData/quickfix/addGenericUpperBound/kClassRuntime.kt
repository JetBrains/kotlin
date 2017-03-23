// "Add 'kotlin.Any' as upper bound for E" "true"
// WITH_RUNTIME
import kotlin.reflect.KClass


inline fun <reified /* abc */   E> bar() = E::class.oldJava<caret>

val <T: Any> KClass<T>.oldJava get() = java
