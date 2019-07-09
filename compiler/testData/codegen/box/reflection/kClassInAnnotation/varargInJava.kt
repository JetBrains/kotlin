// TARGET_BACKEND: JVM

// WITH_RUNTIME
// FILE: Test.java

class O {}
class K {}

@Ann(args={O.class, K.class})
class Test {
}

// FILE: vararg.kt

import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
annotation class Ann(vararg val args: KClass<*>)

fun box(): String {
    val args = Test::class.java.getAnnotation(Ann::class.java).args
    val argName1 = args[0].java.simpleName ?: "fail 1"
    val argName2 = args[1].java.simpleName ?: "fail 2"
    return argName1 + argName2
}
