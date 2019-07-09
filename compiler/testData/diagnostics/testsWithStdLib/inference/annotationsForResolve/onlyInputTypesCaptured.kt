// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// Issue: KT-26698

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
public fun <@kotlin.internal.OnlyInputTypes T> Iterable<T>.contains1(element: T): Boolean = null!!

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
public fun <@kotlin.internal.OnlyInputTypes T> Iterable<T>.foo(element: T): T = null!!

class Inv<T>
class Inv2<T, R>

class In<in T>
class Out<out T>

// -------------------------------------------------------

fun test_0(x: Inv2<in Number, out Number>, list: List<Inv2<Any, Int>>) {
    list.<!NI;TYPE_INFERENCE_ONLY_INPUT_TYPES!>foo<!>(x)
}

// ------------------------- Inv -------------------------

fun test_1(x: Inv<Number>, list: List<Inv<Number>>) {
    list.contains1(x)
}

fun test_2(x: Inv<Number>, list: List<Inv<Int>>) {
    list.<!TYPE_INFERENCE_ONLY_INPUT_TYPES!>contains1<!>(x)
}

fun test_3(x: Inv<Number>, list: List<Inv<Any>>) {
    list.<!TYPE_INFERENCE_ONLY_INPUT_TYPES!>contains1<!>(x)
}

fun test_4(x: Inv<in Number>, list: List<Inv<Any>>) {
    list.<!NI;TYPE_INFERENCE_ONLY_INPUT_TYPES!>contains1<!>(x)
}

fun test_5(x: Inv<in Number>, list: List<Inv<Number>>) {
    list.<!NI;TYPE_INFERENCE_ONLY_INPUT_TYPES!>contains1<!>(x)
}

fun test_6(x: Inv<in Number>, list: List<Inv<Int>>) {
    list.<!TYPE_INFERENCE_ONLY_INPUT_TYPES!>contains1<!>(x)
}

fun test_7(x: Inv<out Number>, list: List<Inv<Any>>) {
    list.<!TYPE_INFERENCE_ONLY_INPUT_TYPES!>contains1<!>(x)
}

fun test_8(x: Inv<out Number>, list: List<Inv<Number>>) {
    list.contains1(x)
}

fun test_9(x: Inv<out Number>, list: List<Inv<Int>>) {
    list.contains1(x)
}

// ------------------------- In -------------------------

fun test_11(x: In<Number>, list: List<In<Number>>) {
    list.contains1(x)
}

fun test_12(x: In<Number>, list: List<In<Int>>) {
    list.contains1(x)
}

fun test_13(x: In<Number>, list: List<In<Any>>) {
    list.contains1(x)
}

// ------------------------- Out -------------------------

fun test_21(x: Out<Number>, list: List<Out<Number>>) {
    list.contains1(x)
}

fun test_22(x: Out<Number>, list: List<Out<Int>>) {
    list.contains1(x)
}

fun test_23(x: Out<Number>, list: List<Out<Any>>) {
    list.contains1(x)
}

// --------------------------------------------------------

fun test_31(x: Inv<Number>, list: List<Inv<in Number>>) {
    list.contains1(x)
}

fun test_32(x: Inv<Number>, list: List<Inv<in Int>>) {
    list.contains1(x)
}

fun test_33(x: Inv<Number>, list: List<Inv<in Any>>) {
    list.<!TYPE_INFERENCE_ONLY_INPUT_TYPES!>contains1<!>(x)
}

fun test_34(x: Inv<Number>, list: List<Inv<out Number>>) {
    list.contains1(x)
}

fun test_35(x: Inv<Number>, list: List<Inv<out Int>>) {
    list.<!TYPE_INFERENCE_ONLY_INPUT_TYPES!>contains1<!>(x)
}

fun test_36(x: Inv<Number>, list: List<Inv<out Any>>) {
    list.contains1(x)
}
