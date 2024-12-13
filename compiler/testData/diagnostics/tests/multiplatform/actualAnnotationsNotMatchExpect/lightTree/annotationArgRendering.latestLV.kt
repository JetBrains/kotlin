// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
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
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> annotation class Ann

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> fun stringConcat() {}

// Not reported in K1, because supported starting from K2
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> fun onType(): Any? = null

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> fun kclassArg() {}
