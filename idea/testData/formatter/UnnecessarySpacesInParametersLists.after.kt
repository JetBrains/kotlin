fun <T> test(a: Int) = {(a: Int) -> a }
class Test<T>

fun foo() {
    test<Int>(12)
}