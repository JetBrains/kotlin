// IGNORE_BACKEND: JS
// WITH_REFLECT

import kotlin.reflect.findAnnotation

annotation class Yes(val value: String)
annotation class No(val value: String)

@Yes("OK")
@No("Fail")
class Foo

fun box(): String {
    return Foo::class.findAnnotation<Yes>()?.value ?: "Fail: no annotation"
}
