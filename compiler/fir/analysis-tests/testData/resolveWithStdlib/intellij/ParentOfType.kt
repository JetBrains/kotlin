// RUN_PIPELINE_TILL: BACKEND
import kotlin.reflect.KClass

fun <T : Number> Any.parentOfTypes(vararg classes: KClass<out T>): T? {
    throw IllegalStateException()
}

val some = "123".parentOfTypes(Int::class, Double::class)
