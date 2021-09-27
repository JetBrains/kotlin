// FIR_IDENTICAL
@Target(AnnotationTarget.TYPE)
annotation class A

typealias Gen<T> = List<@A T>

typealias Test1 = Gen<<!REPEATED_ANNOTATION!>@A<!> Int>
