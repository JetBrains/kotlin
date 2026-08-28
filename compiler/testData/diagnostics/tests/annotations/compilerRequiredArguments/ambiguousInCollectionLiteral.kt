// RUN_PIPELINE_TILL: FRONTEND

package pack

open class Foo {
    class AnnotationTarget
}

class Bar : Foo() {
    @Target(allowedTargets = [AnnotationTarget.<!AMBIGUOUS_ANNOTATION_ARGUMENT!>VALUE_PARAMETER<!>])
    annotation class A
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, collectionLiteral, nestedClass */
