package foo

expect class ExpectInCommonActualInMiddle
expect class ExpectInCommonActualInPlatforms

expect class <!NO_ACTUAL_FOR_EXPECT("class 'ExpectInCommonWithoutActual',  on path common -> jvmAndJs -> js for JS"), NO_ACTUAL_FOR_EXPECT("class 'ExpectInCommonWithoutActual',  on path common -> jvmAndJs -> jvm for JVM, ")!>ExpectInCommonWithoutActual<!>
