// FIR_IDENTICAL
val test: String by materializeDelegate()

fun <T> materializeDelegate(): Delegate<T> = Delegate()

operator fun <K> K.provideDelegate(receiver: Any?, property: kotlin.reflect.KProperty<*>): K = this

operator fun <X> Delegate<X>.getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): X = TODO()

class Delegate<V>
