// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
expect annotation class Ann

@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class Ann2(val s: String)

@Ann2("1" + "2")
expect fun stringConcat()

expect fun onType(): @Ann2("") Any?

annotation class Ann3(val kclass: kotlin.reflect.KClass<*>)

@Ann3(String::class)
expect fun kclassArg()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual annotation class <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT("annotation class Ann : Annotation defined in root package in file common.kt; annotation class Ann : Annotation defined in root package in file jvm.kt; Annotation `@Target(allowedTargets = {AnnotationTarget.FUNCTION, AnnotationTarget.CLASS})` is missing on actual declaration")!>Ann<!>

actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT("fun stringConcat(): Unit defined in root package in file common.kt; fun stringConcat(): Unit defined in root package in file jvm.kt; Annotation `@Ann2(s = \"12\")` is missing on actual declaration")!>stringConcat<!>() {}

// Not reported in K1, because supported starting from K2
actual fun onType(): Any? = null

actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT("fun kclassArg(): Unit defined in root package in file common.kt; fun kclassArg(): Unit defined in root package in file jvm.kt; Annotation `@Ann3(kclass = kotlin.String::class)` is missing on actual declaration")!>kclassArg<!>() {}
