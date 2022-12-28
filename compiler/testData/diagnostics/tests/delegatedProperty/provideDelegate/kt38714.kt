// WITH_STDLIB
// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

abstract class MainActivity : DIAware1() {
    val bar: Bar by <!DEBUG_INFO_LEAKING_THIS!>instance1<!>()
}

class Bar

open class DIAware1 {
    inline fun <reified T : Any> DIAware1.instance1(tag: Any? = null): DIProperty<T> = TODO()
}

class DIProperty<out V> : LazyDelegate<V> {
    override fun provideDelegate(receiver: Any?, prop: KProperty<Any?>): Lazy<V> = TODO()
}

interface LazyDelegate<out V> {
    operator fun provideDelegate(receiver: Any?, prop: KProperty<Any?>): Lazy<V>
}