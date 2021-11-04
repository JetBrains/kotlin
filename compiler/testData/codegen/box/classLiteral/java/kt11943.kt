// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE, WASM

// WITH_RUNTIME

import kotlin.reflect.KClass

val <T : KClass<*>> T.myjava1: Class<*>
    get() = java

val <E : Any, T : KClass<E>> T.myjava2: Class<E>
    get() = java

class O
class K

fun box(): String =
        O::class.myjava1.getSimpleName() + K::class.myjava2.getSimpleName()

