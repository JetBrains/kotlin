// LANGUAGE: +CompanionBlocksAndExtensions
// IGNORE_BACKEND_K1: ANY


typealias Foo<T> = MutableList<T>
typealias Bar = MutableList<Int>

companion fun Foo.foo() = "foo"
companion fun Bar.bar() = "bar"

fun box(): String {
    return if (Foo.foo() == Bar.foo() && Foo.bar() == Bar.bar()) "OK" else "FAIL" //todo: Is it norm?
}
