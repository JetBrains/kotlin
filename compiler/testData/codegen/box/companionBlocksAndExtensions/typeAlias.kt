// LANGUAGE: +CompanionBlocksAndExtensions
// DUMP_KLIB_ABI: DEFAULT


typealias Foo<T> = MutableList<T>
typealias Bar = MutableList<Int>

companion fun Foo.foo() = "foo"
companion fun Bar.bar() = "bar"

fun box(): String {
    return if (
        Foo.foo() == "foo" &&
        Bar.foo() == "foo" &&
        Foo.bar() == "bar" &&
        Bar.bar() == "bar"
    ) "OK" else "FAIL"
}
