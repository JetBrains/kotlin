// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
import kotlin.reflect.KClass

annotation class Ann(val kClass: KClass<*>)

class A {
    @Ann(EmptyList::class)
    fun foo() {}

    object EmptyList
}
