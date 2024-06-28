// FIR_IDENTICAL
import kotlin.reflect.KProperty

fun <X> myEmptyList1(): List<X> = TODO()
fun <Y> myEmptyList2(): List<Y> = TODO()

var b: Boolean = false

val foo: List<String> by myLazy {
    if (b) return@myLazy myEmptyList1()

    myEmptyList2()
}

private fun <T> myLazy(initialValue: () -> T): ReadProperty<T> = TODO()

class ReadProperty<V>(val v: V) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): V = v
}