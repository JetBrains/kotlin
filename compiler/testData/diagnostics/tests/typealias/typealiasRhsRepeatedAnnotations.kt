// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
@Target(AnnotationTarget.TYPE)
annotation class A

typealias Test1 = @A <!REPEATED_ANNOTATION!>@A<!> Int
