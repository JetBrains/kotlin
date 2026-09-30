// IGNORE_BACKEND: JVM
// VALHALLA_VALUE_CLASSES
// LANGUAGE: +FullValueClasses
// JVM_EXPOSE_BOXED
// WITH_STDLIB

@JvmInline
value class Wrapper(val s: String)

value class Pair(val n: Int, val w: Wrapper?)

var log = ""

abstract value class Base(w: Wrapper?) {
    init {
        log += "${w?.s};"
    }
}

value class Derived(val w: Wrapper?) : Base(w)

abstract value class PairBase(a: Int, w: Wrapper?) {
    init {
        log += "$a,${w?.s};"
    }
}

value class Reordered(val v: Int, val w: Wrapper?) : PairBase(w = w, a = if (v > 0) 1 else 2)

fun box(): String {
    val pairs = listOf(Pair(1, Wrapper("a")), Pair(2, null))
    if (pairs.map { it.w?.s } != listOf("a", null)) return "Fail: $pairs"
    val derived = listOf(Derived(Wrapper("b")), Derived(null))
    if (derived.map { it.w?.s } != listOf("b", null)) return "Fail: $derived"
    val reordered = listOf(Reordered(1, Wrapper("c")), Reordered(0, null))
    if (reordered.map { it.w?.s } != listOf("c", null)) return "Fail: $reordered"
    return if (log == "b;null;1,c;2,null;") "OK" else "Fail: $log"
}
