package foo

actual class ExpectInCommonActualInMiddle

expect class ExpectInMiddleActualInPlatforms

expect class <!NO_ACTUAL_FOR_EXPECT("class 'ExpectInMiddleWithoutActual'", " in module js for JS", ""), NO_ACTUAL_FOR_EXPECT("class 'ExpectInMiddleWithoutActual'", " in module jvm for JVM", "")!>ExpectInMiddleWithoutActual<!>
