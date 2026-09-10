// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
// ALLOW_KOTLIN_PACKAGE

package kotlin

import kotlin.reflect.*

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class NumericClass(vararg val actualizations: KClass<*>)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class SymbolicClass(vararg val actualizations: KClass<*>)

open class Base {
    class Int
}

class Derived : Base() {
    @NumericClass(<!AMBIGUOUS_ANNOTATION_ARGUMENT!>Int<!>::class)
    class A

    @SymbolicClass(Int::class)
    class B
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, classReference, nestedClass, outProjection,
primaryConstructor, propertyDeclaration, starProjection, vararg */
