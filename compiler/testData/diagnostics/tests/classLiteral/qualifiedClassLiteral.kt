// RUN_PIPELINE_TILL: CODEGEN
package test.foo.bar

class A

val k = test.foo.bar.A::class

val l = java.lang.Class::class

/* GENERATED_FIR_TAGS: classDeclaration, classReference, propertyDeclaration, starProjection */
