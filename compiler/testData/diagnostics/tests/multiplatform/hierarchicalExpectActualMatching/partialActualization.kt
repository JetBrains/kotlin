// !LANGUAGE: +MultiPlatformProjects
// FILE: common.kt
package foo

expect class ExpectInCommonActualInMiddle

expect class ExpectInCommonActualInJvm
expect class ExpectInCommonActualInJs

expect class ExpectInCommonWithoutActual


// FILE: jvmAndJs.kt
package foo

actual class ExpectInCommonActualInMiddle

expect class ExpectInMiddleActualInJvm
expect class ExpectInMiddleActualInJs

expect class ExpectInMiddleWithoutActual


// FILE: jvm.kt
package foo

actual class ExpectInCommonActualInJvm
actual class ExpectInMiddleActualInJs

expect class ExpectInJvmWithoutActual

expect class ExpectInJvmActualInJvm
actual class ExpectInJvmActualInJvm


// FILE: js.kt
package foo

actual class ExpectInCommonActualInJs
actual class ExpectInMiddleActualInJs

expect class ExpectInJsWithoutActual

expect class ExpectInJsActualInJs
actual class ExpectInJsActualInJs