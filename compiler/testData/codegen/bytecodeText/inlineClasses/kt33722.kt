// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

fun foo() {
    val result = Result.success("yes!")
    val other = Result.success("nope")

    result == other
    result != other

    result.equals(other)
    !result.equals(other)
}

// CHECK_BYTECODE_TEXT
// 0 INVOKESTATIC kotlin/Result.box-impl
// 4 INVOKESTATIC kotlin/Result.equals-impl0