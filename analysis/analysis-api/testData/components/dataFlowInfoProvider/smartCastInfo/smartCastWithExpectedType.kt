interface A
abstract class B : A
abstract class C : B()

fun test(a: A) {}

fun usage(b: B) {
    if (b is C) {
        test(<expr>b</expr>)
    }
}