// See also KT-10869: Accessing lazy properties from init causes IllegalArgumentException

import kotlin.reflect.KProperty

class CustomDelegate {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = prop.name
}

class Kaboom() {
    // Here and below we should have errors for simple AND delegated
    init {
        delegated.hashCode()
        simple.hashCode()
        withGetter.hashCode()
    }

    val other = delegated

    val another = simple

    val something = withGetter
    
    val delegated: String by CustomDelegate()

    val simple = "xyz"

    val withGetter: String
        get() = "abc"

    // No error should be here
    val after = delegated
}
