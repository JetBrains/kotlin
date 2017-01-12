// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_REFLECT

import kotlin.reflect.*

class A<T> {
    val result = "OK"
}

fun box(): String {
    val k: KProperty1<A<*>, *> = A::class.memberProperties.single()
    return k.get(A<String>()) as String
}
