// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
public fun <@kotlin.internal.OnlyInputTypes T> Iterable<T>.contains1(element: T): Boolean = null!!

class In<in T>

class Out<out T>

class Inv<T>

fun test_1(list: List<In<Number>>, x: In<Number>, y: In<Int>, z: In<Any>) {
    list.contains1(x)
    list.contains1(y)
    list.contains1(z)
}

fun test_2(list: List<In<Int>>, x: In<Int>, y: In<Number>, z: In<Any>) {
    list.contains1(x)
    list.contains1(y)
    list.contains1(z)
}

fun test_3(list: List<Out<Number>>, x: Out<Number>, y: Out<Int>, z: Out<Any>) {
    list.contains1(x)
    list.contains1(y)
    list.contains1(z)
}

fun test_4(list: List<Out<Int>>, x: Out<Int>, y: Out<Number>, z: Out<Any>) {
    list.contains1(x)
    list.contains1(y)
    list.contains1(z)
}

fun test_5(list: List<Inv<Number>>, x: Inv<Number>, y: Inv<Int>, z: Inv<Any>) {
    list.contains1(x)
    list.<!TYPE_INFERENCE_ONLY_INPUT_TYPES!>contains1<!>(y)
    list.<!TYPE_INFERENCE_ONLY_INPUT_TYPES!>contains1<!>(z)
}

fun test_6(list: List<Inv<Int>>, x: Inv<Int>, y: Inv<Number>, z: Inv<Any>) {
    list.contains1(x)
    list.<!TYPE_INFERENCE_ONLY_INPUT_TYPES!>contains1<!>(y)
    list.<!TYPE_INFERENCE_ONLY_INPUT_TYPES!>contains1<!>(z)
}
