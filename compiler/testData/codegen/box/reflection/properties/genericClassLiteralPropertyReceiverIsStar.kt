// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.full.*

class A<T> {
    val result = "OK"
}

fun box(): String {
    val k: KProperty1<A<*>, *> = A::class.memberProperties.single()
    return k.get(A<String>()) as String
}
