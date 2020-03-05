package foo

actual class ExpectInCommonActualInPlatforms
actual class ExpectInMiddleActualInPlatforms

expect class <!NO_ACTUAL_FOR_EXPECT("class 'ExpectInJvmWithoutActual'", "jvm for JVM", "")!>ExpectInJvmWithoutActual<!>

expect class ExpectInJvmActualInJvm
actual class ExpectInJvmActualInJvm
