// RUN_PIPELINE_TILL: FRONTEND
open class A(val x: Any)

class B : A(<!NO_THIS!>this<!>::class)

/* GENERATED_FIR_TAGS: classDeclaration, classReference, primaryConstructor, propertyDeclaration, thisExpression */
