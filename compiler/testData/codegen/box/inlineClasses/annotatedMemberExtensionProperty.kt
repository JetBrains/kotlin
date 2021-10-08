// WITH_RUNTIME

@Target(AnnotationTarget.PROPERTY)
annotation class Anno

@JvmInline
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
