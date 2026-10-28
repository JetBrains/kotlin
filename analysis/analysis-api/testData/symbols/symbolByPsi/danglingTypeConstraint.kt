fun <T> foo(a: AA, t: T): AA where AA : Number = a

class Foo<T> where AA : Any

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Anno

fun <T, U> annotated(t: T, u: U) where @Anno T : Any, @Anno AA : Any {}
