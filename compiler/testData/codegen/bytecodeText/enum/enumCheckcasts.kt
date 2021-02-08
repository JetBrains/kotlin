enum class Foo {
    A, B, C { override fun result() = "OK" };
    open fun result() = "Fail"
}

// There are two CHECKCASTs, one in Foo.valueOf and one in Foo.values
// 2 CHECKCAST
