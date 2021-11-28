// IGNORE_BACKEND: JS, JS_IR, WASM
// IGNORE_BACKEND: JS_IR_ES6
// WITH_REFLECT

package test

import kotlin.reflect.typeOf
import kotlin.reflect.KClass
import kotlin.reflect.KType

interface KFunction<out R>

val <T : Any> KClass<T>.primaryConstructor0: KFunction<T>
    get() = object : KFunction<T> {}

interface A<out T : Any> {
    val t: T
}

inline fun <reified T : KFunction<E>, E : Any> bar(w: A<E>): Pair<KType, KFunction<E>> {
    val q: KFunction<E> = w.t::class.primaryConstructor0
    return typeOf<T>() to q
}

inline fun <reified Q> typeOfValue(q: Q): KType {
    return typeOf<Q>()
}

fun box(): String {
    val q: A<*> = object : A<CharSequence> {
        override val t: CharSequence
            get() = ""
    }
    val (w, f) = bar(q) // T should be inferred to KFunction<Captured(*)> and should be approximated to KFunction<Any>, not KFunction<*>

    val expected = "test.KFunction<kotlin.Any>"
    if (w.toString() != expected) return "Fail 1: $w"
    if (typeOfValue(f).toString() != expected) return "Fail 2: $f"
    return "OK"
}
