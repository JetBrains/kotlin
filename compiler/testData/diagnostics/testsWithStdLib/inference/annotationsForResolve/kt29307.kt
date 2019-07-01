// !DIAGNOSTICS: -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE
// ISSUE: KT-29307

fun test_1(map: Map<String, String>) {
    val x = <!NI;TYPE_INFERENCE_ONLY_INPUT_TYPES!>map[<!OI;CONSTANT_EXPECTED_TYPE_MISMATCH!>42<!>]<!> // OK
}

open class A

class B : A()

fun test_2(map: Map<A, String>) {
    val x = <!NI;TYPE_INFERENCE_ONLY_INPUT_TYPES!>map[<!OI;CONSTANT_EXPECTED_TYPE_MISMATCH!>42<!>]<!>
}

fun test_3(m: Map<*, String>) {
    val x = <!NI;TYPE_INFERENCE_ONLY_INPUT_TYPES!>m[42]<!> // should be ok
}

fun test_4(m: Map<out Number, String>) {
    val x = m.<!NI;TYPE_INFERENCE_ONLY_INPUT_TYPES!>get<!>(42) // should be ok
}

fun test_5(map: Map<B, Int>, a: A) {
    map.get(a)
}

fun test_6(map: Map<A, Int>, b: B) {
    map.get(b)
}