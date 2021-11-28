// WITH_STDLIB

val test1: Map<String, String> by lazy(LazyThreadSafetyMode.NONE) {
    mapOf("string" to "string").mapValues { it.toString() }
}

val test2: String by myDelegate("OK")

fun <T> myDelegate(initializer: T): Delegate<T> = Delegate(initializer)

class Delegate<T>(val initializer: T) {
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): T {
        return initializer
    }
}

operator fun <T : Any> T.provideDelegate(receiver: Any?, property: kotlin.reflect.KProperty<*>): T = this

fun box(): String {
    test1
    return test2
}