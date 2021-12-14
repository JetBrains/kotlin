// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

@Target(AnnotationTarget.PROPERTY)
annotation class Anno

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z(val s: String)

class A {
    @Anno
    val Z.r: String get() = s
}

fun box(): String {
    with(A()) {
        return Z("OK").r
    }
}
