// FIR_IDENTICAL
// IGNORE_BACKEND: JKLIB
import kotlin.reflect.KClass

annotation class A(val klass: KClass<*>)

class C

@A(C::class) fun test1() {}
