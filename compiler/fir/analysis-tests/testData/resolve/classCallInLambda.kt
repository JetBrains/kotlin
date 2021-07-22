// WITH_STDLIB
// DUMP_CFG
import kotlin.reflect.KClass

fun test(x: String): KClass<*> {
    return x.let { it::class }
}
