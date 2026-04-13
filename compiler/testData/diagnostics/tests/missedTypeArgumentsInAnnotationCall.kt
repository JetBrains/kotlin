// RUN_PIPELINE_TILL: FRONTEND
package usage

annotation class B<T>

@<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>B<!>
class A

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, nullableType, typeParameter */
