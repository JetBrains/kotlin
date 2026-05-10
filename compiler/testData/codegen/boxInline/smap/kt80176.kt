// FILE: 1.kt
import kotlin.properties.*
import kotlin.reflect.KProperty

interface Element
abstract class Container
class Strategy {
    companion object {
        val DEFAULT = Strategy()
    }
}
class DelegateProvider<T> {
    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadOnlyProperty<Any?, Any?> = TODO()
}

context(container: Container)
inline fun <reified P : Element, A> error1(
    strategy: Strategy = Strategy.DEFAULT
): DelegateProvider<A> {
    return DelegateProvider<A>()
}

// FILE: 2.kt
interface Symbol

internal object ContainerObj: Container() {
    val P1 by error1<Element, Symbol>()
    val P2 by error1<Element, Symbol>()
}

fun box() = "OK"
