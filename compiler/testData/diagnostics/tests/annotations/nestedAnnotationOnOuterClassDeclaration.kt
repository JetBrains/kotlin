// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-64059

interface OuterInterface

class MyClass: @MyClass.NestedAnnotation OuterInterface {

    @Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
    annotation class NestedAnnotation
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, interfaceDeclaration, nestedClass */
