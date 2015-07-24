import kotlin.test.assertEquals

annotation(retention = AnnotationRetention.SOURCE) class SourceAnno
annotation(retention = AnnotationRetention.BINARY) class BinaryAnno
annotation(retention = AnnotationRetention.RUNTIME) class RuntimeAnno

@SourceAnno
@BinaryAnno
@RuntimeAnno
fun box(): String {
    assertEquals(listOf(javaClass<RuntimeAnno>()), ::box.annotations.map { it.annotationType() })
    return "OK"
}
