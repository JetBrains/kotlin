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
    assertEquals(listOf(javaClass<RuntimeAnno>()), ::box.annotations.map { it.annotationType() })
    return "OK"
}
