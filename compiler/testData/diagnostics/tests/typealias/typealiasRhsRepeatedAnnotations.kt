// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
@Target(AnnotationTarget.TYPE)
annotation class A

typealias Test1 = @A <!REPEATED_ANNOTATION!>@A<!> Int
