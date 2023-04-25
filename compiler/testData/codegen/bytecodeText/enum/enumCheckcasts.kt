enum class Foo {
    A, B, C { override fun result() = "OK" };
    open fun result() = "Fail"
}

// JVM_TEMPLATES
// There are two CHECKCASTs, one in Foo.valueOf and one in Foo.values
// 2 CHECKCAST

// JVM_IR_TEMPLATES
// For JVM IR, there's an additional checkcast of `$ENTRIES` to `[Ljava/lang/Enum;` in the static initializer.
// 3 CHECKCAST
