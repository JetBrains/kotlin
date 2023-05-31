// PLATFORM_DEPENDANT_METADATA
// ALLOW_AST_ACCESS

package test

import kotlin.reflect.KProperty

annotation class Anno

class CustomDelegate {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = prop.name
}

class Class {
    @delegate:Anno val property by CustomDelegate()
}
