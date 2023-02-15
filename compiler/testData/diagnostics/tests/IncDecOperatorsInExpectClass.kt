// In FIR, declaring the same `expect` and `actual` classes in one compiler module is not possible (see KT-55177). Hence the `.fir.kt` test
// expects a diagnostic here. The multi-module test corresponding to this test is called: `multiplatform/incDecOperatorsInExpectClass.kt`.

// !LANGUAGE: +MultiPlatformProjects
// SKIP_TXT
// Issue: KT-49714

expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>class Counter<!> {
    operator fun inc(): Counter
    operator fun dec(): Counter
}

actual typealias <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>Counter<!> = Int
