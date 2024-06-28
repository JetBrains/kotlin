// Test for ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT diagnostic when annotations arguments are lazily resolved.

// MODULE: common
// TARGET_PLATFORM: Common
@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class Ann(val s: String = "default")

expect fun onType_negative(): @Ann("") Any
expect fun onType_positive(): @Ann("") Any

@Ann("")
expect fun onFunction_negative()
@Ann("")
expect fun onFunction_positive()

@Ann
expect fun withEmptyArguments_negative()
@Ann
expect fun withEmptyArguments_positive()

// MODULE: main()()(common)
// TARGET_PLATFORM: JVM
actual fun onType_negative(): @Ann("") Any = Any()
actual fun onType_positive(): @Ann("incorrect") Any = Any()

@Ann("")
actual fun onFunction_negative() {}
@Ann("incorrect")
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>onFunction_positive<!>() {}

@Ann
actual fun withEmptyArguments_negative() {}
@Ann("incorrect")
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>withEmptyArguments_positive<!>() {}
