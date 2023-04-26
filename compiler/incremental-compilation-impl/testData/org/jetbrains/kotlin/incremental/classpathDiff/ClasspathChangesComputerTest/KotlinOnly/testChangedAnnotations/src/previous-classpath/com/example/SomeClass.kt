package com.example

@AnnotationOne // Will change to @AnnotationTwo
class SomeClassWithChangedAnnotation {
    val unchangeProperty = 0
    fun unchangedFunction() {}
}

class SomeClass {

    @AnnotationOne // Will change to @AnnotationTwo
    val propertyWithChangedAnnotation = 0

    @AnnotationOne // Will change to @AnnotationTwo
    fun functionWithChangedAnnotation() {
    }
}

annotation class AnnotationOne
annotation class AnnotationTwo