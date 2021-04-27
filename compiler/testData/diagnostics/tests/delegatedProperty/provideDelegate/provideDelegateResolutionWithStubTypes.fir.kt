val test: String by <!TYPE_MISMATCH!>materializeDelegate()<!>

fun <T> materializeDelegate(): Delegate<T> = Delegate()

operator fun <K> K.provideDelegate(receiver: Any?, property: kotlin.reflect.KProperty<*>): K = this

class Delegate<V> {
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): V = TODO()
}
