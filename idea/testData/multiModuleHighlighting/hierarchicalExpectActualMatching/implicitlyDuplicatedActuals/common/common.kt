package foo

// implicitly: only one duplicated declaration have 'actual' modifier, but both are matched

expect class <!AMBIGUOUS_ACTUALS("Class 'ActualInMiddleCompatibleInJs'", "jvmAndJs, js for JS")!>ActualInMiddleCompatibleInJs<!>
expect class <!AMBIGUOUS_ACTUALS("Class 'CompatibleInMiddleActualInJvm'", "jvmAndJs, jvm for JVM")!>CompatibleInMiddleActualInJvm<!>

expect class <!AMBIGUOUS_ACTUALS("Class 'CompatibleInMiddleAndPlatforms'", "jvmAndJs, js for JS"), AMBIGUOUS_ACTUALS("Class 'CompatibleInMiddleAndPlatforms'", "jvmAndJs, jvm for JVM")!>CompatibleInMiddleAndPlatforms<!>