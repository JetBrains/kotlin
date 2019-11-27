// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KFunction1
import kotlin.reflect.jvm.isAccessible

fun doStuff(fn: KFunction1<String, String>) = fn.call("oK")

fun box(): String {
    return doStuff(String::capitalize.apply { isAccessible = true })
}
