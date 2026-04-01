// SKIP_JDK6
// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// PARAMETERS_METADATA
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class A() {
    suspend fun test(OK: String) {

    }
}

fun box(): String {
    val clazz = A::class.java
    val method = clazz.getDeclaredMethod("test", String::class.java, Continuation::class.java)
    val parameters = method.getParameters()

    if (parameters[0].modifiers != 0) return "wrong modifier on value parameter: ${parameters[0].modifiers}"
    if (parameters[1].modifiers != 0) return "wrong modifier on Continuation parameter: ${parameters[1].modifiers}"
    return parameters[0].name
}
