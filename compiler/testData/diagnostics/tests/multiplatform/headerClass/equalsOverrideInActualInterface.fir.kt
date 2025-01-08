// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
expect interface Base

// MODULE: m1-jvm()()(m1-common)
actual interface Base {
    override fun <!ACTUAL_WITHOUT_EXPECT!>equals<!>(other: Any?): Boolean
}
