// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidAnnotationsWithUseSiteTargetOnExpressions
// ISSUE: KT-75242

annotation class Decl

fun test(x: Int) {
    if (x > 10)
        <!ANNOTATION_WITH_USE_SITE_TARGET_ON_EXPRESSION_ERROR!>@all:Decl<!> { }

    for (i in 1..10) <!ANNOTATION_WITH_USE_SITE_TARGET_ON_EXPRESSION_ERROR!>@field:Decl<!> { }

    when (x) {
        1 -> <!ANNOTATION_WITH_USE_SITE_TARGET_ON_EXPRESSION_ERROR!>@setparam:Decl<!> { "" }
    }
}

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION)
annotation class Source

fun test() {
    <!ANNOTATION_WITH_USE_SITE_TARGET_ON_EXPRESSION_ERROR!>@all:Source<!>
    when { else -> {} }

    <!ANNOTATION_WITH_USE_SITE_TARGET_ON_EXPRESSION_ERROR!>@get:Source<!>
    while (true) { break }

    <!ANNOTATION_WITH_USE_SITE_TARGET_ON_EXPRESSION_ERROR!>@receiver:Source<!>
    for (i in 1..10) {}
}

/* GENERATED_FIR_TAGS: annotationDeclaration, annotationUseSiteTargetAll, annotationUseSiteTargetField,
annotationUseSiteTargetPropertyGetter, annotationUseSiteTargetReceiver, annotationUseSiteTargetSetterParameter, break,
comparisonExpression, equalityExpression, forLoop, functionDeclaration, ifExpression, integerLiteral, localProperty,
propertyDeclaration, rangeExpression, stringLiteral, whenExpression, whenWithSubject, whileLoop */
