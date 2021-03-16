// IGNORE_DUPLICATED_FIR_SOURCE_EXCEPTION
// FIR_COMPARISON
import kotlin.reflect.KClass

annotation class Ann(val value: KClass<*>)

@Ann(Array<<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: String123">String123</error>>::class) class A
