import kotlin.reflect.KClass

annotation class A(val a: Int, val c: KClass<*>)

@A(1, Int::class)
class F<caret>oo