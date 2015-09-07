import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
annotation class Ann(val arg: KClass<*>)

fun box(): String {
    val argName = javaClass<Test>().getAnnotation(javaClass<Ann>()).arg.simpleName ?: "fail 1"
    return argName
}
