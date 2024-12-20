// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73256
// LANGUAGE: -AnnotationAllUseSiteTarget

annotation class Default

<!UNSUPPORTED_FEATURE!>@all:Default<!>
class My(<!UNSUPPORTED_FEATURE!>@all:Default<!> val x: Int)

<!UNSUPPORTED_FEATURE!>@all:Default<!>
fun foo(): <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@all:Default<!> Int {
    <!UNSUPPORTED_FEATURE!>@all:Default<!> val x = 0
    val y = @all:Default x
    return y
}
