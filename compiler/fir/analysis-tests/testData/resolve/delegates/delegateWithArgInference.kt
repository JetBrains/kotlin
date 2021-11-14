import kotlin.reflect.KProperty

class Delegate<T>(val data: T) {
    operator fun getValue(thisRef: Nothing?, prop: KProperty<*>): T = data
}

fun makeIntDelegate(t: Int): Delegate<Int> = Delegate(t)
fun <TT> makeDelegate(t: TT): Delegate<TT> = Delegate(t)
fun <M> materialize(): M = null!!
fun <M2> materialize2(): M2 = null!!
fun <Q> id(v: Q): Q = v

val x by makeIntDelegate(run {
    val x: String = materialize()
    materialize2()
})
