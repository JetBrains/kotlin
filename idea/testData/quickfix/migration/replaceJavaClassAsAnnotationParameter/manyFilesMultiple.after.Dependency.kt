import kotlin.reflect.KClass

annotation class AnnO(val arg: KClass<*>)

AnnO(Int::class)
Ann1(String::class)
class MyClassO
