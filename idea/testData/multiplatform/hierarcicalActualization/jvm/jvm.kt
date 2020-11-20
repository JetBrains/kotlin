package foo

actual class ExpectInCommonActualInPlatforms
actual class ExpectInMiddleActualInPlatforms

expect class <!NO_ACTUAL_FOR_EXPECT!>ExpectInJvmWithoutActual<!>

expect class ExpectInJvmActualInJvm
actual class ExpectInJvmActualInJvm
