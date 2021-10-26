// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: TYPEOF
// WITH_RUNTIME

// See KT-37163

import kotlin.reflect.typeOf

class In<in T>

interface A
interface B
class C() : A, B

// TODO check real effects to fix the behavior when we reach consensus
//  and to be sure that something is not dropped by optimizations.

var l = ""
fun log(s: String) {
    l += s + ";"
}

fun consume(a: Any?) {}

@OptIn(kotlin.ExperimentalStdlibApi::class)
inline fun <reified K> select(x: K, y: Any): K where K : A, K : B {
    log((x is K).toString())
    log((y is K).toString())
    consume(K::class)
    log("KClass was created")
    consume(typeOf<K>())
    log("KType was created")
    consume(Array<K>(1) { x })
    log("array was created")
    return x as K
}

fun test(a: Any, b: Any) {
    if (a is A && a is B) {
        select(a, b)
    }
}

fun box(): String {
    test(C(), object : A, B {})
    test(C(), object : A {})
    test(C(), object : B {})
    test(C(), object {})
    test(C(), Any())

//    if (
//        l != "true;true;KClass was created;KType was created;array was created;" +
//             "true;false;KClass was created;KType was created;array was created;" +
//             "true;false;KClass was created;KType was created;array was created;" +
//             "true;false;KClass was created;KType was created;array was created;" +
//             "true;false;KClass was created;KType was created;array was created;"
//    ) {
//        return "fail: $l"
//    }

    return "OK"
}
