package com.example

@AnnotationTwo // Changed from @AnnotationOne
class SomeClassWithChangedAnnotation {
    val unchangeProperty = 0
    fun unchangedFunction() {}
}

class SomeClass {

    @AnnotationTwo // Changed from @AnnotationOne
    val propertyWithChangedAnnotation = 0

    @AnnotationTwo // Changed from @AnnotationOne
    fun functionWithChangedAnnotation() {
    }
}

annotation class AnnotationOne
annotation class AnnotationTwo