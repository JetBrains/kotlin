package foo

actual class ExpectInCommonActualInPlatforms

actual class ExpectInMiddleActualInPlatforms

expect class <!NO_ACTUAL_FOR_EXPECT("class 'ExpectInJsWithoutActual'", " in module js for JS", "")!>ExpectInJsWithoutActual<!>

expect class ExpectInJsActualInJs
actual class ExpectInJsActualInJs