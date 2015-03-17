fun main(args: Array<String>) {
    val p: Foo = Foo() // simple class usage

    // default object usages
    Foo.f()
    val x = Foo

    Foo.Default.f()
    val xx = Foo.Default
}