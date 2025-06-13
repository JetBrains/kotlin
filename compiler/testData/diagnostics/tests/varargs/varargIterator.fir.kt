// RUN_PIPELINE_TILL: BACKEND
package test

class CompositeIterator<T>(vararg iterators: java.util.Iterator<T>){
    val iteratorsIter = iterators.iterator()
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, nullableType, outProjection, primaryConstructor,
propertyDeclaration, typeParameter, vararg */
