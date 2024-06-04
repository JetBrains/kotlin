import kotlin.reflect.KClass

annotation class A(val a: Int, val c: KClass<*>)

data class Foo(@param:A(1, Int::class) val ba<caret>r: String)