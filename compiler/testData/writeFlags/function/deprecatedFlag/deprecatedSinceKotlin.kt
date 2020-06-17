@Deprecated("")
@DeprecatedSinceKotlin(warningSince = "1.0")
fun test1() {}

@Deprecated("")
@DeprecatedSinceKotlin(warningSince = "1.9")
fun test2() {}

@Deprecated("")
@DeprecatedSinceKotlin
fun test3() {}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: DeprecatedSinceKotlinKt, test1
// FLAGS: ACC_DEPRECATED, ACC_PUBLIC, ACC_FINAL, ACC_STATIC

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: DeprecatedSinceKotlinKt, test2
// FLAGS: ACC_DEPRECATED, ACC_PUBLIC, ACC_FINAL, ACC_STATIC

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: DeprecatedSinceKotlinKt, test3
// FLAGS: ACC_DEPRECATED, ACC_PUBLIC, ACC_FINAL, ACC_STATIC
