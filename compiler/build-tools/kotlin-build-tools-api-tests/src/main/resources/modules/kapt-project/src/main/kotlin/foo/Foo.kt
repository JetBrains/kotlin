package foo

@Target(AnnotationTarget.CLASS)
annotation class MyAnnotation

@MyAnnotation
class Foo
