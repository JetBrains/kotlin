// KT-40412

// WITH_STDLIB

// MODULE: lib
// FILE: lib.kt

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

var result = "FAIL"

class CaptureContext<A>(val capture: (A) -> Unit) : ReadOnlyProperty<A, () -> Unit> {
    override fun getValue(thisRef: A, property: KProperty<*>) = { -> capture(thisRef) }
}
operator fun <A> ((A) -> Unit).provideDelegate(thisRef: A, property: KProperty<*>) = CaptureContext(this)

fun right(arg: Right) { result = "OK" }
class Right { val prop: () -> Unit by ::right }

// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    val r = Right()
    r.prop()
    return result
}