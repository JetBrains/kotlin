import kotlin.reflect.KClass
import java.lang.annotation.*

Retention(RetentionPolicy.RUNTIME)
annotation
class Ann(val arg: KClass<*>)

class OK

Ann(OK::class) class MyClass

fun box(): String {
    val argName = javaClass<MyClass>().getAnnotation(javaClass<Ann>()).arg.simpleName ?: "fail 1"
    return argName
}
