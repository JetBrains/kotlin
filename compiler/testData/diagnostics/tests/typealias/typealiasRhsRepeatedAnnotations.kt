@Target(AnnotationTarget.TYPE)
annotation class A

typealias Test1 = @A <!REPEATED_ANNOTATION!>@A<!> Int
