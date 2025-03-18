// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ForbidAnnotationsWithUseSiteTargetOnExpressions
// ISSUE: KT-75242

annotation class Decl

fun test(x: Int) {
    if (x > 10)
        <!ANNOTATION_WITH_USE_SITE_TARGET_ON_EXPRESSION_WARNING!>@all:Decl<!> { }

    for (i in 1..10) <!ANNOTATION_WITH_USE_SITE_TARGET_ON_EXPRESSION_WARNING!>@field:Decl<!> { }

    when (x) {
        1 -> <!ANNOTATION_WITH_USE_SITE_TARGET_ON_EXPRESSION_WARNING!>@setparam:Decl<!> { "" }
    }
}

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION)
annotation class Source

fun test() {
    <!ANNOTATION_WITH_USE_SITE_TARGET_ON_EXPRESSION_WARNING!>@all:Source<!>
    when { else -> {} }

    <!ANNOTATION_WITH_USE_SITE_TARGET_ON_EXPRESSION_WARNING!>@get:Source<!>
    while (true) { break }

    <!ANNOTATION_WITH_USE_SITE_TARGET_ON_EXPRESSION_WARNING!>@receiver:Source<!>
    for (i in 1..10) {}
}
