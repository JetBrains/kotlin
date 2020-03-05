// !DIAGNOSTICS: -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE
// !LANGUAGE: -NonStrictOnlyInputTypesChecks
// ISSUE: KT-29307

fun test_1(map: Map<String, String>) {
    val x = map[42] // OK
}

open class A

class B : A()

fun test_2(map: Map<A, String>) {
    val x = map[42]
}

fun test_3(m: Map<*, String>) {
    val x = m[42] // should be ok
}

fun test_4(m: Map<out Number, String>) {
    val x = m.get(42) // should be ok
}

fun test_5(map: Map<B, Int>, a: A) {
    map.get(a)
}

fun test_6(map: Map<A, Int>, b: B) {
    map.get(b)
}