// ISSUE: KT-60510
interface T

interface A : T, (String) -> Unit
interface B : T, (Int) -> Unit

fun test_1(f: T) {
    when(f) {
        is A -> f("Hello")
        is B -> f(42)
    }
}

class Box(val f: T) {
    fun test_2() {
        when (f) {
            is A -> f("Hello")
            is B -> f(42)
        }
    }
}
