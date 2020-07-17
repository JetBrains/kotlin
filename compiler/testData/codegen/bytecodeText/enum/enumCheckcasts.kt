enum class Foo {
    A, B, C { override fun result() = "OK" };
    open fun result() = "Fail"
}

// JVM_TEMPLATES:
// There are two CHECKCASTs, one in Foo.valueOf and one in Foo.values
// 2 CHECKCAST

// JVM_IR_TEMPLATES:
// There should be only one CHECKCAST in Foo.valueOf
// 1 CHECKCAST