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
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT("Ann; Ann; Annotation `@Target(allowedTargets = [AnnotationTarget.FUNCTION, AnnotationTarget.CLASS])` is missing on actual declaration")!>actual annotation class Ann<!>

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT("stringConcat; stringConcat; Annotation `@Ann2(s = \"12\")` is missing on actual declaration")!>actual fun stringConcat() {}<!>

// Not reported in K1, because supported starting from K2
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT("onType; onType; Annotation `@Ann2(s = \"\")` is missing on actual declaration")!>actual fun onType(): Any? = null<!>

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT("kclassArg; kclassArg; Annotation `@Ann3(kclass = String::class)` is missing on actual declaration")!>actual fun kclassArg() {}<!>
