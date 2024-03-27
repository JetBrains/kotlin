// !DIAGNOSTICS: -UNUSED_PARAMETER
// WITH_STDLIB

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class CaptureContext<A>(val capture: (A) -> Unit) : ReadOnlyProperty<A, () -> Unit> {
    override fun getValue(thisRef: A, property: KProperty<*>) = { -> capture(thisRef) }
}
operator fun <A> ((A) -> Unit).provideDelegate(thisRef: A, property: KProperty<*>) = CaptureContext(this)

fun wrong(arg: Wrong) {}
class Wrong

class Right {
    val prop: () -> Unit by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>::wrong<!>
}

fun box(): String {
    Right().prop()
    return "OK"
}
