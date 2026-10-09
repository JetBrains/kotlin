// ISSUE: KT-89993
// VALHALLA_VALUE_CLASSES
// LANGUAGE: +FullValueClasses
// WITH_STDLIB

var log = ""

abstract value class Base(b: Int) {
    init {
        log += "$b;"
    }
}

abstract value class BooleanBase(b: Boolean) {
    init {
        log += "$b;"
    }
}

abstract value class NullableBase(a: Any?, b: Any) {
    init {
        log += "$a,$b;"
    }
}

abstract value class PairBase(a: Int, b: Int) {
    init {
        log += "$a,$b;"
    }
}

fun traced(tag: String, value: Int): Int {
    log += tag
    return value
}

abstract value class GenericBase<T>(t: T) {
    init {
        log += "$t;"
    }
}

@JvmInline
value class Wrapper(val s: String)

value class IfArgument(val x: Int) : Base(if (x > 0) 1 else 2)

value class ElvisArgument(val x: Int?) : Base(x ?: 0)

value class WhenArgument(val x: Int) : Base(when (x) { 0 -> 10; else -> 20 })

value class ComparisonArgument(val x: Int) : BooleanBase(x > 0)

value class InlinedLoopArgument(val x: Int) : Base(listOf(x).map { it * 2 }.first())

value class NullableInlineArgument(val w: Wrapper?, val v: Wrapper) : NullableBase(w, v)

value class GenericInlineArgument(val w: Wrapper?) : GenericBase<Wrapper?>(w)

value class TryArgument(val s: String) : Base(try { s.toInt() } catch (e: NumberFormatException) { -1 })

value class ReorderedArguments(val x: Int) : PairBase(b = traced("b", if (x > 0) 1 else 2), a = traced("a", 3))

fun box(): String {
    val values = listOf(
        IfArgument(5).x, ElvisArgument(null).x, WhenArgument(0).x, ComparisonArgument(1).x, InlinedLoopArgument(3).x, TryArgument("s").s,
        ReorderedArguments(4).x,
    )
    if (values != listOf(5, null, 0, 1, 3, "s", 4)) return "Fail: $values"
    val wrappers = listOf(NullableInlineArgument(Wrapper("a"), Wrapper("b")).w, GenericInlineArgument(null).w)
    if (wrappers != listOf(Wrapper("a"), null)) return "Fail: $wrappers"
    return if (log == "1;0;10;true;6;-1;ba3,1;Wrapper(s=a),Wrapper(s=b);null;") "OK" else "Fail: $log"
}
