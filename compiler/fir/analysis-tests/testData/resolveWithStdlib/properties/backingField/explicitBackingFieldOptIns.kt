// RUN_PIPELINE_TILL: FRONTEND

@RequiresOptIn
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER)
annotation class ExperimentalAPI

@ExperimentalAPI
val numbersA: List<Int>
    field = mutableListOf()

val numbersB: List<Int>
    @ExperimentalAPI
    field = mutableListOf()

fun test() {
    val a: MutableList<Int> = <!OPT_IN_USAGE_ERROR!>numbersA<!>
    val b: MutableList<Int> = <!OPT_IN_USAGE_ERROR!>numbersB<!>
}

object MyObject {
    val x: Number
        @ExperimentalAPI
        field = 123.4
}

fun foo() {
    if (MyObject.x is Float) {
        MyObject.x.isNaN()
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, explicitBackingField, functionDeclaration, ifExpression, isExpression,
localProperty, objectDeclaration, propertyDeclaration, smartcast */
