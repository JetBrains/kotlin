// In FIR, declaring the same `expect` and `actual` classes in one compiler module is not possible (see KT-55177). Hence the `.fir.kt` test
// expects a diagnostic here. The multi-module test corresponding to this test is called: `multiplatform/incDecOperatorsInExpectClass.kt`.

// !LANGUAGE: +MultiPlatformProjects
// SKIP_TXT
// Issue: KT-49714

expect class Counter {
    operator fun inc(): Counter
    operator fun dec(): Counter
}

actual typealias <!ACTUAL_WITHOUT_EXPECT!>Counter<!> = Int
