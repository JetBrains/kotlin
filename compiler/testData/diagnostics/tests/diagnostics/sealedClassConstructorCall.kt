// RUN_PIPELINE_TILL: FRONTEND
sealed class A

val b = <!INVISIBLE_REFERENCE!>A<!>()

/* GENERATED_FIR_TAGS: classDeclaration, propertyDeclaration, sealed */
