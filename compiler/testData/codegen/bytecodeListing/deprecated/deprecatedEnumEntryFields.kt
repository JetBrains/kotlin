// WITH_STDLIB
// IGNORE_BACKEND_K2: JVM_IR
// FIR status: KT-57216 K2: non-trivial enum declaration does not have ACC_FINAL in the bytecode

enum class Test {
    @Deprecated("") ENTRY1,
    ENTRY2,
    @Deprecated("") ENTRY3
}
