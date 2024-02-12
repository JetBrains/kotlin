@Target(AnnotationTarget.TYPE)
annotation class A

typealias Gen<T> = List<@A T>

typealias Test1 = <!REPEATED_ANNOTATION!>Gen<@A Int><!>
