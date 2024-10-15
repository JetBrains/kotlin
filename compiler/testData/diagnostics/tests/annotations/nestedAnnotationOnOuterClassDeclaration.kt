// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-64059

interface OuterInterface

class MyClass: @MyClass.NestedAnnotation OuterInterface {

    @Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
    annotation class NestedAnnotation
}
