@Target(AnnotationTarget.TYPE)
annotation class Anno

class A<T>

fun foo(x: <expr> @Anno A<A<*>> </expr>) {}
