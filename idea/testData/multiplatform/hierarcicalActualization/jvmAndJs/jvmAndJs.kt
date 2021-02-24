package foo

actual class <!LINE_MARKER("descr='Has declaration in common module'")!>ExpectInCommonActualInMiddle<!>

expect class <!LINE_MARKER("descr='Has actuals in JVM, JS'")!>ExpectInMiddleActualInPlatforms<!>

expect class <!NO_ACTUAL_FOR_EXPECT, NO_ACTUAL_FOR_EXPECT!>ExpectInMiddleWithoutActual<!>
