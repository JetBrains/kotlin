// MODULE: m1-common
// FILE: common.kt
expect val <!NO_ACTUAL_FOR_EXPECT{JVM}!>String<!>.a : String

expect val <!NO_ACTUAL_FOR_EXPECT{JVM}!>b<!> : String

expect val c : String.() -> String

expect val d : (String) -> String

expect val <T> T.e : String

expect val <T> <!NO_ACTUAL_FOR_EXPECT{JVM}!>T<!>.f : String

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual val <!ACTUAL_WITHOUT_EXPECT!>a<!> : String = ""

actual val <!ACTUAL_WITHOUT_EXPECT!>String<!>.b : String
    get() = ""

actual val c : (String) -> String
    get() = {""}

actual val d : String.() -> String
    get() = {""}

actual val <T> T.e : String
    get() = ""

actual val <!ACTUAL_WITHOUT_EXPECT!>f<!> : String = ""
