// TARGET_BACKEND: JVM
// FILE: A.kt

package a

@Retention(AnnotationRetention.RUNTIME)
annotation class Ann

interface Tr {
    @Ann
    fun foo() {}
}

// FILE: B.kt

class C : a.Tr

fun box(): String {
    val method = C::class.java.getDeclaredMethod("foo")
    val annotations = method.getDeclaredAnnotations().joinToString("\n")
    if (annotations != "@a.Ann()") {
        return "Fail: $annotations"
    }
    return "OK"
}
