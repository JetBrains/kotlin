// CHOOSE_USE_SITE_TARGET: param
// IS_APPLICABLE: false

@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)
annotation class C

class Extension

fun @C<caret> Extension.foo(): String = ""
