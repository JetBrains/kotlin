@Target(AnnotationTarget.TYPE)
annotation class A

typealias Test1 = @A @A Int
