// WITH_RUNTIME

// See KT-37128

import kotlin.reflect.typeOf

// TODO check real effects to fix the behavior when we reach consensus
//  and to be sure that something is not dropped by optimizations.

@OptIn(kotlin.ExperimentalStdlibApi::class)
inline fun <reified T> T.causeBug() {
    val x = this
    x is T
    x as T
    T::class
    Array<T>(1) { x }

    // Non-reified type parameters with recursive bounds are not yet supported, see Z from class Something
    // typeOf<T>()
}

interface SomeToImplement<SELF_TVAR>

class Y : SomeToImplement<Y>

class Something<Z> where Z : SomeToImplement<Z> {
    fun op() = causeBug()
}

fun box(): String {
    Something<Y>().op()
    return "OK"
}
