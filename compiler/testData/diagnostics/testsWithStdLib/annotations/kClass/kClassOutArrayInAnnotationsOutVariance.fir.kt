import kotlin.reflect.KClass

open class A
class B1 : A()
class B2 : A()

annotation class Ann1(val arg: Array<out KClass<out A>>)
