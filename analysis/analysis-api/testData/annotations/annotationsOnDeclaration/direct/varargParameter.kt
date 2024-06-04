import kotlin.reflect.KClass

annotation class A(val a: Int, vararg val cs: KClass<*>)

@A(a = 1, Int::class, String::class)
fun fo<caret>o(): Int = 42
