import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
annotation class Ann(val arg: KClass<*>)

fun box(): String {
    val argName = Test::class.java.getAnnotation(Ann::class.java).arg.simpleName ?: "fail 1"
    return argName
}
