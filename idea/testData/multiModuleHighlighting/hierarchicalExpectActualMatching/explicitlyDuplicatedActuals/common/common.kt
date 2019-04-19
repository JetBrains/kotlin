package foo

// explicitly: both duplicated declaration have 'actual' modifier
// implicitly: only one duplicated declaration have 'actual' modifier, but both are matched

expect class <!AMBIGUOUS_ACTUALS("Class 'ExplicitlyDuplicatedByMiddleAndJs'", "jvmAndJs, js for JS")!>ExplicitlyDuplicatedByMiddleAndJs<!>

expect class <!AMBIGUOUS_ACTUALS("Class 'ExplicitlyDuplicatedByMiddleAndJvm'", "jvmAndJs, jvm for JVM")!>ExplicitlyDuplicatedByMiddleAndJvm<!>
