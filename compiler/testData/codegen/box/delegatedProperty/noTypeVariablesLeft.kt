import kotlin.reflect.KProperty

var foo: (() -> String)? by property(null)

private fun <T> property(initialValue: T): RwProperty<T> = RwProperty(initialValue)

class RwProperty<V>(var v: V) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): V = v

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
        this.v = value
    }
}

fun box(): String {
    foo = { "OK" }
    return foo!!.invoke()
}