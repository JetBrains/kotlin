// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// Test for ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT diagnostic when annotations arguments are lazily resolved.

// MODULE: common
@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Ann<!>(val s: String = "default")

<!CONFLICTING_OVERLOADS!>expect fun onType_negative(): @Ann("") Any<!>
<!CONFLICTING_OVERLOADS!>expect fun onType_positive(): @Ann("") Any<!>

<!CONFLICTING_OVERLOADS!>@Ann("")
expect fun onFunction_negative()<!>
<!CONFLICTING_OVERLOADS!>@Ann("")
expect fun onFunction_positive()<!>

<!CONFLICTING_OVERLOADS!>@Ann
expect fun withEmptyArguments_negative()<!>
<!CONFLICTING_OVERLOADS!>@Ann
expect fun withEmptyArguments_positive()<!>

// MODULE: main()()(common)
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
