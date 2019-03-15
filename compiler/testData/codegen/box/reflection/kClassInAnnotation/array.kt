// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
annotation class Ann(val args: Array<KClass<*>>)

class O
class K

@Ann(arrayOf(O::class, K::class)) class MyClass

fun box(): String {
    val args = MyClass::class.java.getAnnotation(Ann::class.java).args
    val argName1 = args[0].simpleName ?: "fail 1"
    val argName2 = args[1].simpleName ?: "fail 2"
    return argName1 + argName2
}
