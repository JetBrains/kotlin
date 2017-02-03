// IGNORE_BACKEND: JS
// WITH_REFLECT

import kotlin.reflect.KFunction1

fun doStuff(fn: KFunction1<String, String>) = fn.call("ok")

fun box(): String {
    return doStuff(String::toUpperCase)
}
