import kotlin.test.assertEquals

annotation(retention = AnnotationRetention.RUNTIME) class Anno

fun box(): String {
    val a = Anno::class.annotations
/*
    // TODO: support kotlin.annotation.annotation
    if (a.size() != 1) return "Fail 1: $a"
    val ann = a.single() as? annotation ?: return "Fail 2: ${a.single()}"
    assertEquals(AnnotationRetention.RUNTIME, ann.retention)
*/
    assert(a.isEmpty())
    return "OK"
}
