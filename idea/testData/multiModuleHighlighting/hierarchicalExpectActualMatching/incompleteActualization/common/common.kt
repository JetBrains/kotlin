package foo

expect class <!NO_ACTUAL_FOR_EXPECT("class 'ExpectInCommonActualInJsOnly'", " on path common -> jvmAndJs -> jvm for JVM", "")!>ExpectInCommonActualInJsOnly<!>
expect class <!NO_ACTUAL_FOR_EXPECT("class 'ExpectInCommonActualInJvmOnly'", " on path common -> jvmAndJs -> js for JS", "")!>ExpectInCommonActualInJvmOnly<!>
