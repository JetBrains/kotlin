// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-63063

class Test {
    @ClassObjectAnnotation
    @NestedAnnotation
    companion object {
        annotation class ClassObjectAnnotation
    }

    annotation class NestedAnnotation
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, companionObject, nestedClass, objectDeclaration */
