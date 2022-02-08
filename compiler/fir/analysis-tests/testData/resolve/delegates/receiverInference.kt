import kotlin.reflect.KProperty


interface XEntity
class Provider<out R> {
    operator fun provideDelegate(thisRef: XEntity, prop: Any): R = null!!
}

class Prop<E, V> {
    operator fun getValue(receiver: E, prop: Any): V = null!!
}

fun <R: XEntity, V> mkProp(): Provider<Prop<R, V>> = Provider()

class MyEnt: XEntity {
    val d: String by mkProp()
}