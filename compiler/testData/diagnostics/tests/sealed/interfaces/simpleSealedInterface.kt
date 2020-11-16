// ISSUE: KT-20423
// !LANGUAGE: +FreedomForSealedClasses +SealedInterfaces
// !DIAGNOSTICS: -UNUSED_VARIABLE

sealed interface Base

interface A : Base

sealed class B : Base {
    class First : B()
    class Second : B()
}

enum class C : Base {
    SomeValue, AnotherValue
}

object D : Base

fun test_1(base: Base) {
    val x = when (base) {
        is A -> 1
        is B -> 2
        is C -> 3
        is D -> 4
    }
}

fun test_2(base: Base) {
    val x = <!NO_ELSE_IN_WHEN!>when<!> (base) {
        is A -> 1
        is B.First -> 2
        is B.Second -> 3
        C.SomeValue -> 4
        C.AnotherValue -> 5
        D -> 6
    }
}
