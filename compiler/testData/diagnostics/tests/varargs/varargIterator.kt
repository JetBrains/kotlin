// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
package test

class CompositeIterator<T>(vararg iterators: <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.util.Iterator<T><!>){
    val iteratorsIter = iterators.iterator()
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, nullableType, outProjection, primaryConstructor,
propertyDeclaration, typeParameter, vararg */
