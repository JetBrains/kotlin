package foo

expect class ExpectInCommonActualInMiddle
expect class ExpectInCommonActualInPlatforms

expect class <!NO_ACTUAL_FOR_EXPECT("class 'ExpectInCommonWithoutActual'", "js for JS", ""), NO_ACTUAL_FOR_EXPECT("class 'ExpectInCommonWithoutActual'", "jvm for JVM", "")!>ExpectInCommonWithoutActual<!>
