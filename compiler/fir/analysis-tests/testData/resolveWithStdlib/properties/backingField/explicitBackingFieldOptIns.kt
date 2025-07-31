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

/* GENERATED_FIR_TAGS: annotationDeclaration, annotationUseSiteTargetPropertyGetter, functionDeclaration, localProperty,
propertyDeclaration, smartcast */
