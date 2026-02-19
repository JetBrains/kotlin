// RUN_PIPELINE_TILL: FRONTEND
open class A(val x: Any)

class B : A(<!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>::class)

/* GENERATED_FIR_TAGS: classDeclaration, classReference, primaryConstructor, propertyDeclaration, thisExpression */
