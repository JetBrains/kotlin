// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
@Target(AnnotationTarget.TYPE)
annotation class A

typealias Test1 = @A <!REPEATED_ANNOTATION!>@A<!> Int
