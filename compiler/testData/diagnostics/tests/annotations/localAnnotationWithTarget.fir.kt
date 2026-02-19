// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76300
fun main() {
    @Target(AnnotationTarget.LOCAL_VARIABLE)
    annotation <!LOCAL_ANNOTATION_CLASS_ERROR!>class PropertyOnly<!>
}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, localClass */
