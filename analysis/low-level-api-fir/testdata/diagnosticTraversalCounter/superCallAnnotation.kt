@Target(AnnotationTarget.TYPE)
annotation class Anno

open class A

class B : @Anno A()