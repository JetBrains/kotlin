// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

val b: First by lazy {
    object : First {   }
}

private val withoutType by lazy {
    object : First { }
}

private val withTwoSupertypes by lazy {
    object : First, Second { }
}

interface First
interface Second

fun <T> lazy(initializer: () -> T): Lazy<T> = TODO()
interface Lazy<out T> {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = TODO()
}