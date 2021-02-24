package foo

actual class ExpectInCommonActualInPlatforms

actual class <!LINE_MARKER("descr='Has declaration in common module'")!>ExpectInMiddleActualInPlatforms<!>

expect class <!NO_ACTUAL_FOR_EXPECT!>ExpectInJsWithoutActual<!>

expect class <!LINE_MARKER("descr='Has actuals in JS'")!>ExpectInJsActualInJs<!>
actual class <!LINE_MARKER!>ExpectInJsActualInJs<!>