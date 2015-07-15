import kotlin.reflect.KClass

annotation(retention = AnnotationRetention.RUNTIME)
class Ann(vararg val args: KClass<*>)

fun box(): String {
    val args = javaClass<Test>().getAnnotation(javaClass<Ann>()).args
    val argName1 = args[0].simpleName ?: "fail 1"
    val argName2 = args[1].simpleName ?: "fail 2"
    return argName1 + argName2
}
