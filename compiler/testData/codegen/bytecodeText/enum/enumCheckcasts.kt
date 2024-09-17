enum class Foo {
    A, B, C { override fun result() = "OK" };
    open fun result() = "Fail"
}

// There are three CHECKCASTs: one in Foo.valueOf, one in Foo.values, and one in the static initializer
// (`$ENTRIES` to `[Ljava/lang/Enum;`).
// 3 CHECKCAST
