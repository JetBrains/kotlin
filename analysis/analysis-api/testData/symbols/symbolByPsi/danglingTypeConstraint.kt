fun <T> foo(a: AA, t: T): AA where AA : Number = a

class Foo<T> where AA : Any

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Anno

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Declared

fun <T, U> annotated(t: T, u: U) where @Anno T : Any, @Anno AA : Any {}

fun <@Declared T> annotatedDeclaration(t: T) where @Anno T : Any {}

class AnnotatedClass<@Declared T> where @Anno T : Any, @Anno AA : Any
