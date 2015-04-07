import kotlin.reflect.KClass
import java.lang.annotation.*

Retention(RetentionPolicy.RUNTIME)
annotation
class Ann(vararg val args: KClass<*>)

class O
class K

Ann(O::class, K::class) class MyClass

fun box(): String {
    val args = javaClass<MyClass>().getAnnotation(javaClass<Ann>()).args
    val argName1 = args[0].simpleName ?: "fail 1"
    val argName2 = args[1].simpleName ?: "fail 2"
    return argName1 + argName2
}
