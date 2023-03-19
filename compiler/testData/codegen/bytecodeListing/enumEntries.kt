// !LANGUAGE: +EnumEntries
// TARGET_BACKEND: JVM_IR
// FULL_JDK
// WITH_STDLIB

// IGNORE_BACKEND_K2: JVM_IR
// FIR status: KT-57216 K2: non-trivial enum declaration does not have ACC_FINAL in the bytecode

enum class SimpleEnum {
    A, B, C
}

enum class WithConstructor(val x: String) {
    A("1"), B("2"), C("3")
}

enum class WithEntryClass {
    A {
        override fun foo() {}
    }
    ;
    abstract fun foo()
}

annotation class Ann

enum class WithAnnotations {
    @Ann A, @Ann B
}
