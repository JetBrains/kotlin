// TARGET_BACKEND: JVM
// WITH_REFLECT
// WITH_STDLIB
// FILE: J.java
public interface J<T> {
    public T getValue();
}

// FILE: box.kt
class Impl(val x: String) : J<String> {
    override fun getValue() = x
}

val j1: J<String> = Impl("O")
// Note that taking a reference to `J<T>::value` is not permitted by the frontend
// in any context except as a direct argument to `by`; e.g. `val x by run { j1::value }`
// would produce an error.
val x by j1::value

@Target(AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.EXPRESSION, AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
annotation class Anno

fun box(): String {
    val j2: J<String> = Impl("K")
    val y by j2::value
    val y1 by @Anno j2::value
    val y2 by (j2::value)
    val y3 by (j2)::value
    val y4 by ((j2)::value)
    val y5 by (((j2)::value))
    val y6 by @Anno() (((j2)::value))
    val y7 by (@Anno() ((j2)::value))
    val y8 by ((@Anno() (j2)::value))
    val y9 by @Anno() (j2)::value
    return x + y
}
