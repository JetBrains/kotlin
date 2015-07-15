import kotlin.reflect.*
import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

fun foo() {}
fun Int.bar() {}
val baz = 42
val Int.quux: Int get() = this

fun box(): String {
    fun check(actual: Collection<KCallable<*>>, expected: Set<String>) {
        assertEquals(expected, actual.map { it.name }.toSet())
    }

    val kp = Class.forName("_DefaultPackage").kotlinPackage ?: return "Fail: package class not found"

    check(kp.members, setOf("bar", "baz", "foo", "box", "quux"))
    check(kp.functions, setOf("bar", "foo", "box"))
    check(kp.properties, setOf("baz"))
    check(kp.extensionProperties, setOf("quux"))

    return "OK"
}
