// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.test.assertEquals

@Retention(AnnotationRetention.SOURCE)
annotation class SourceAnno

@Retention(AnnotationRetention.BINARY)
annotation class BinaryAnno

@Retention(AnnotationRetention.RUNTIME)
annotation class RuntimeAnno

@SourceAnno
@BinaryAnno
@RuntimeAnno
fun box(): String {
    assertEquals(listOf(RuntimeAnno::class.java), ::box.annotations.map { it.annotationClass.java })
    return "OK"
}
