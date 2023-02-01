interface Foo<out T>
interface Bar1 : Foo<Bar1>
interface Bar2 : Foo<Bar2>
class Bar3 : Foo<Bar3>

fun test(b1: Bar1, b2: Bar2, b3: Bar3) {
    b1 == b2
    b1 == b3
}
