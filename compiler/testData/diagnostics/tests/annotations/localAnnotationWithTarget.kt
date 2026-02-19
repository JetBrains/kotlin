// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76300
fun main() {
    <!LOCAL_ANNOTATION_CLASS_ERROR!>@Target(AnnotationTarget.LOCAL_VARIABLE)
    annotation class PropertyOnly<!>
}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, localClass */
