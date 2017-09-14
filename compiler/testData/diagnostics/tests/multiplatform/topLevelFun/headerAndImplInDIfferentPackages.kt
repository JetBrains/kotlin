// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt
package common

<!JS:HEADER_WITHOUT_IMPLEMENTATION, JVM:HEADER_WITHOUT_IMPLEMENTATION!>header fun foo()<!>

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt
package jvm

<!IMPLEMENTATION_WITHOUT_HEADER!>impl fun foo()<!> {}

// MODULE: m3-js(m1-common)
// FILE: js.kt
package js

<!IMPLEMENTATION_WITHOUT_HEADER!>impl fun foo()<!> {}
