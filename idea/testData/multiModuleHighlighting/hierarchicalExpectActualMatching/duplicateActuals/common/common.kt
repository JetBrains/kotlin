package foo

// explicitly: both duplicated declaration have 'actual' modifier
// implicitly: only one duplicated declaration have 'actual' modifier, but both are matched

expect class <error descr="[AMBIGUOUS_ACTUALS] Class 'ExplicitlyDuplicatedByMiddleAndJs' has several compatible actual declarations in modules jvmAndJs, js for JS">ExplicitlyDuplicatedByMiddleAndJs</error>
expect class <error descr="[AMBIGUOUS_ACTUALS] Class 'ImplicitlyDuplicatedByMiddleAndJs' has several compatible actual declarations in modules jvmAndJs, js for JS">ImplicitlyDuplicatedByMiddleAndJs</error>

expect class <error descr="[AMBIGUOUS_ACTUALS] Class 'ExplicitlyDuplicatedByMiddleAndJvm' has several compatible actual declarations in modules jvmAndJs, jvm for JVM">ExplicitlyDuplicatedByMiddleAndJvm</error>
expect class <error descr="[AMBIGUOUS_ACTUALS] Class 'ImplicitlyDuplicatedByMiddleAndJvm' has several compatible actual declarations in modules jvmAndJs, jvm for JVM">ImplicitlyDuplicatedByMiddleAndJvm</error>