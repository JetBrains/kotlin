// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt
<!EXPECT_ACTUAL_MISMATCH{JVM}!>expect<!> val String.a : String

<!EXPECT_ACTUAL_MISMATCH{JVM}!>expect<!> val b : String

expect val c : String.() -> String

expect val d : (String) -> String

expect val <T> T.e : String

<!EXPECT_ACTUAL_MISMATCH{JVM}!>expect<!> val <T> T.f : String

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual val <!ACTUAL_WITHOUT_EXPECT!>a<!> : String = ""

actual val String.<!ACTUAL_WITHOUT_EXPECT!>b<!> : String
    get() = ""

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> val c : (String) -> String
    <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>get<!>() = {""}

actual val d : String.() -> String
    get() = {""}

actual val <T> T.e : String
    get() = ""

actual val <!ACTUAL_WITHOUT_EXPECT!>f<!> : String = ""
