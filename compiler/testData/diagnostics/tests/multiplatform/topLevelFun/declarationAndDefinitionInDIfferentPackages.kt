// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt
package common

platform fun foo()

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt
package jvm

<!PLATFORM_DEFINITION_WITHOUT_DECLARATION!>impl<!> fun foo() {}

// MODULE: m3-js(m1-common)
// FILE: js.kt
package js

<!PLATFORM_DEFINITION_WITHOUT_DECLARATION!>impl<!> fun foo() {}
