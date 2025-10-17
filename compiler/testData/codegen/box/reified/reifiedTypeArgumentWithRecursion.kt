// IGNORE_BACKEND_K2_MULTI_MODULE: JVM_IR, JVM_IR_SERIALIZE
// ^^^KT-79201: This function has a reified type parameter and thus can only be inlined at compilation time, not called directly.

// WITH_STDLIB

// See KT-37128

// FILE: lib.kt
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

// FILE: main.kt
interface SomeToImplement<SELF_TVAR>

class Y : SomeToImplement<Y>

class Something<Z> where Z : SomeToImplement<Z> {
    fun op() = causeBug()
}

fun box(): String {
    Something<Y>().op()
    return "OK"
}
