import kotlin.reflect.KProperty

fun <T> lazy(initializer: () -> T): Lazy<T> = Lazy(initializer())

class Lazy<T>(val value: T)

inline operator fun <T> Lazy<T>.getValue(thisRef: Any?, property: KProperty<*>): T = value

class A {
    val i by lazy {
        1
    }
}