// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME
// FILE: Test.java

class OK {}

@Ann(arg=OK.class)
class Test {
}

// FILE: basic.kt

import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
annotation class Ann(val arg: KClass<*>)

fun box(): String {
    val argName = Test::class.java.getAnnotation(Ann::class.java).arg.java.simpleName ?: "fail 1"
    return argName
}
