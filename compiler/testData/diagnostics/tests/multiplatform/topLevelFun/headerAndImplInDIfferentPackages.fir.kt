// MODULE: m1-common
// FILE: common.kt
package common

expect fun foo()

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
package jvm

<!ACTUAL_WITHOUT_EXPECT!>actual fun foo() {}<!>

// MODULE: m3-js()()(m1-common)
// FILE: js.kt
package js

<!ACTUAL_WITHOUT_EXPECT!>actual fun foo() {}<!>
