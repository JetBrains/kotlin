// TARGET_BACKEND: JVM
// WITH_REFLECT

@Retention(AnnotationRetention.RUNTIME)
annotation class Simple(val value: String)

interface A<T>

@Simple("OK")
public val <T> A<T>.p: String
    get() = TODO()

fun box(): String {
    val o = object : A<Int> {}
    return (o::p.annotations.single() as Simple).value
}
