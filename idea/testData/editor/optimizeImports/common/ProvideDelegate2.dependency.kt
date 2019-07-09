package a

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class C
operator fun C.provideDelegate(thisRef: Any, prop: KProperty<*>): ReadOnlyProperty<Any, C> = TODO()