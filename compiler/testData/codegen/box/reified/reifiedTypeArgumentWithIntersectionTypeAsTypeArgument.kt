// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: TYPEOF
// WITH_RUNTIME

// See KT-37163

import kotlin.reflect.typeOf

class In<in T>

interface A
interface B

// TODO check real effects to fix the behavior when we reach consensus
//  and to be sure that something is not dropped by optimizations.

@OptIn(kotlin.ExperimentalStdlibApi::class)
inline fun <reified K> select(x: K, y: K) {
    x is K
    x as K
    K::class
    typeOf<K>()
    Array<K>(1) { x }
}
fun test() {
    select(In<A>(), In<B>())
}

fun box(): String {
    test()
    return "OK"
}
