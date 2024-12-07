// SKIP_WHEN_OUT_OF_CONTENT_ROOT

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun resolve<caret>Me() {
    receive(valueWithExplicitType)
    receive(valueWithImplicitType)

    variableWithExplicitType = 10
    variableWithImplicitType = 10
}

fun receive(value: Int){}

val delegate = object: ReadWriteProperty<Any?, Int> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Int = 1
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {}
}

val valueWithExplicitType: Int by delegate
val valueWithImplicitType by delegate

var variableWithExplicitType: Int by delegate
var variableWithImplicitType by delegate
