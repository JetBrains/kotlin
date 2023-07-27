import kotlin.reflect.KProperty

class LazyDelegate<T>(val value: T) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value
}

fun <T> lazy(block: () -> T): LazyDelegate<T> = LazyDelegate(block())

fun getAny(): Any? = null

fun <Q> materialize(): Q = null!!

class Test {
    val <caret>resolveMe: String by lazy {
        materialize()
    }
}
