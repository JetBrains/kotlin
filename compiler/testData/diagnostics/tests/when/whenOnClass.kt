// FIR_IDENTICAL
// FULL_JDK
// WITH_STDLIB
// WITH_REFLECT

interface A
interface B : A
interface C

fun test1(a: A) {
    when (a.javaClass) {
        A::class.java -> {}
        B::class.java -> {}
        C::class.java -> {}
        Any::class.java -> {}
        else -> {}
    }
}

class Foo : B
class Bar

fun test2(f: Foo) {
    when (f.javaClass) {
        Foo::class.java -> {}
        Bar::class.java -> {}
        A::class.java -> {}
        B::class.java -> {}
        C::class.java -> {}
        Any::class.java -> {}
    }
}
