// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KFunction1
import kotlin.reflect.jvm.isAccessible

fun doStuff(fn: KFunction1<String, String>) = fn.call("oK")

fun box(): String {
    val method: KFunction1<String, String> = String::capitalize
    return doStuff(method.apply { isAccessible = true })
}
