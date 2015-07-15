import kotlin.test.assertEquals

class Klass

fun box(): String {
    assertEquals("Klass", Klass::class.simpleName)
    assertEquals("Date", java.util.Date::class.simpleName)
    assertEquals("Kind", kotlin.jvm.internal.KotlinSyntheticClass.Kind::class.simpleName)
    assertEquals("Void", java.lang.Void::class.simpleName)

    return "OK"
}
