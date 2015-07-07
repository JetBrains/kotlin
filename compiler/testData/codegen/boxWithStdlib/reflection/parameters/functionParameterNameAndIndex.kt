import kotlin.reflect.*
import kotlin.test.assertEquals

fun foo(bar: String): Int = bar.length()

class A(val c: String) {
    fun foz(baz: Int) {}
}

fun Int.qux(zux: String) {}

fun checkParameters(f: KFunction<*>, names: List<String?>) {
    val params = f.parameters
    assertEquals(names, params.map { it.name })
    assertEquals((0..params.size() - 1).toList(), params.mapIndexed { index, element -> index })
}

fun box(): String {
    checkParameters(::box, listOf())
    checkParameters(::foo, listOf("bar"))
    checkParameters(A::foz, listOf(null, "baz"))
    checkParameters(Int::qux, listOf(null, "zux"))

    checkParameters(::A, listOf("c"))

    return "OK"
}
