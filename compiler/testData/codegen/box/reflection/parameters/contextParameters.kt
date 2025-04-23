// LANGUAGE: +ContextParameters
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: ANY
// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.KParameter.Kind.*
import kotlin.test.assertEquals

annotation class P(val value: String)

interface A
interface B
interface C
object D

class Z {
    context(@P("a") a: A, @P("b") b: B?)
    fun C.f(@P("d") d: D) {}

    @setparam:P("d")
    context(@P("a") a: A, @P("b") b: B?)
    var C.p: D
        get() = D
        set(d) {}
}

fun checkABC(params: List<KParameter>) {
    assertEquals(listOf(INSTANCE, CONTEXT, CONTEXT, EXTENSION_RECEIVER), params.map { it.kind })
    assertEquals(listOf(null, "a", "b", null), params.map { it.name })
    assertEquals(listOf(typeOf<Z>(), typeOf<A>(), typeOf<B?>(), typeOf<C>()), params.map { it.type })
    assertEquals(params.indices.toList(), params.map { it.index })

    assertEquals(
        listOf(emptyList(), listOf("a"), listOf("b"), emptyList()),
        params.map { param -> param.annotations.map { (it as P).value } },
    )
}

fun checkABCD(params: List<KParameter>) {
    assertEquals(listOf(INSTANCE, CONTEXT, CONTEXT, EXTENSION_RECEIVER, VALUE), params.map { it.kind })
    assertEquals(listOf(null, "a", "b", null, "d"), params.map { it.name })
    assertEquals(listOf(typeOf<Z>(), typeOf<A>(), typeOf<B?>(), typeOf<C>(), typeOf<D>()), params.map { it.type })
    assertEquals(params.indices.toList(), params.map { it.index })

    assertEquals(
        listOf(emptyList(), listOf("a"), listOf("b"), emptyList(), listOf("d")),
        params.map { param -> param.annotations.map { (it as P).value } },
    )
}

fun box(): String {
    val f = Z::class.members.single { it.name == "f" }
    checkABCD(f.parameters)

    val p = Z::class.members.single { it.name == "p" } as KMutableProperty<*>
    checkABC(p.parameters)
    checkABC(p.getter.parameters)
    checkABCD(p.setter.parameters)
    return "OK"
}
