// MODULE: m1-common
// FILE: common.kt
package common

expect fun <!NO_ACTUAL_FOR_EXPECT{JVM}, NO_ACTUAL_FOR_EXPECT{JS}!>foo<!>()

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
package jvm

actual fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>() {}

// MODULE: m3-js()()(m1-common)
// FILE: js.kt
package js

actual fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>() {}
