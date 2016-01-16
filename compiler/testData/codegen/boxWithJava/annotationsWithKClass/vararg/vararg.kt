import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
annotation class Ann(vararg val args: KClass<*>)

fun box(): String {
    val args = Test::class.java.getAnnotation(Ann::class.java).args
    val argName1 = args[0].simpleName ?: "fail 1"
    val argName2 = args[1].simpleName ?: "fail 2"
    return argName1 + argName2
}
