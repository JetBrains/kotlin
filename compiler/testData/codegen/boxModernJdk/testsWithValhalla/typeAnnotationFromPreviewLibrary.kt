// IGNORE_BACKEND: JVM
// EMIT_JVM_TYPE_ANNOTATIONS
// MODULE: lib
// FILE: lib.kt
package lib

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class TypeAnn

// MODULE: main(lib)
// FILE: main.kt
import lib.TypeAnn

class Holder {
    fun f(): @TypeAnn String = "OK"
}

fun box(): String {
    val annotations = Holder::class.java.getMethod("f").annotatedReturnType.annotations.joinToString()
    return if (annotations == "@lib.TypeAnn()") "OK" else "Fail: $annotations"
}
