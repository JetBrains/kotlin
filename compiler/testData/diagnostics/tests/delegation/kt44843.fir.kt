// WITH_STDLIB

// FILE: test.kt
val bar2 by bar2<!NO_VALUE_FOR_PARAMETER!>()<!>

// FILE: lt/neworld/compiler/Foo.kt
package lt.neworld.compiler

class Foo {
    val bar by bar<!NO_VALUE_FOR_PARAMETER!>()<!>
}

// FILE: lt/neworld/compiler/bar/Bar.kt
package lt.neworld.compiler.bar

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun <T, V> bar() = Bar<T, V>()

class Bar<T, V> : ReadOnlyProperty<T, V> {
    override fun getValue(thisRef: T, property: KProperty<*>): V {
        TODO("Not yet implemented")
    }
}
