import kotlin.reflect.KClass

annotation(retention = AnnotationRetention.RUNTIME)
class Ann(val arg: KClass<*>)

fun box(): String {
    val argName = javaClass<Test>().getAnnotation(javaClass<Ann>()).arg.simpleName ?: "fail 1"
    return argName
}
