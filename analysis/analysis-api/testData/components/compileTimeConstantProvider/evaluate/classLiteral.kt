import kotlin.reflect.KClass

annotation class Anno(val value: KClass<*>)

@Anno(<expr>String::class</expr>)
fun test() {}