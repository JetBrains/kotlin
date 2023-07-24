// TARGET_BACKEND: NATIVE

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

import kotlin.concurrent.*
import kotlin.reflect.*

class Box(@Volatile var value1: Int, @Volatile var value2: Int)

inline fun wrapCas(crossinline refGetter: () -> KMutableProperty0<Int>, expected: Int, new: Int) = refGetter().compareAndExchangeField(expected, new)
inline fun wrapCasTwice(crossinline refGetter: () -> KMutableProperty0<Int>, expected: Int, new: Int) = wrapCas(refGetter, expected, new)

class A(@Volatile var x: String) {
    fun add(str: String) {
        update({ -> (this::x)}) { x + str }
    }
}

inline fun update(crossinline refGetter: () -> KMutableProperty0<String>, action: (String) -> String) {
    while (true) {
        val cur = refGetter().get()
        val upd = action(cur)
        if (refGetter().compareAndSetField(cur, upd)) return
    }
}

fun box() : String {
    val box = Box(1, 2)
    wrapCas({ -> (box::value1)}, 1, 3)
    wrapCas({ -> (box::value2)}, 2, 4)
    if (box.value1 != 3) return "FAIL 1"
    if (box.value2 != 4) return "FAIL 2"
    wrapCasTwice({ -> (box::value1)}, 3, 5)
    wrapCasTwice( { -> (box::value2)}, 4, 6)
    if (box.value1 != 5) return "FAIL 3"
    if (box.value2 != 6) return "FAIL 4"

    val a = A("a")
    a.add("b")
    if (a.x != "ab") return "FAIL 5"
    return "OK"
}

