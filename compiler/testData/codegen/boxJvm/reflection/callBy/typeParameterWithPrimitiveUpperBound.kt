// TARGET_BACKEND: JVM
// WITH_REFLECT
import kotlin.reflect.KFunction
import kotlin.reflect.KVariance
import kotlin.test.assertEquals

fun <T : Long> f(vararg values: T): String = "OK"

fun box(): String {
    val f: (Array<Long>) -> String = ::f
    f as KFunction<String>

    assertEquals("fun f(kotlin.Array<out T>): kotlin.String", f.toString())

    val p = f.parameters.single().type
    assertEquals(Array<Long>::class, p.classifier)
    assertEquals(KVariance.OUT, p.arguments.single().variance)
    assertEquals(f.typeParameters.single(), p.arguments.single().type?.classifier)

    return f.callBy(emptyMap())
}
