// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +CompanionBlocks
// OPT_IN: kotlin.ExperimentalContextParameters

import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.full.staticFunctions
import kotlin.test.assertEquals

class Ctx(val v: String)

class C {
    companion {
        context(c: Ctx)
        fun f(x: Int, y: Int = 2): String = "f${c.v}$x$y"

        context(c: Ctx)
        val p: String get() = "p${c.v}"
    }
}

fun box(): String {
    val ctx = Ctx("R")

    val f = C::class.staticFunctions.single { it.name == "f" }
    assertEquals(listOf(KParameter.Kind.CONTEXT, KParameter.Kind.VALUE, KParameter.Kind.VALUE), f.parameters.map { it.kind })
    assertEquals("fR12", f.call(ctx, 1, 2))
    assertEquals("fR12", f.callBy(mapOf(f.parameters[0] to ctx, f.parameters[1] to 1)))

    // Context properties are found among members and take the context argument.
    val p = C::class.members.single { it.name == "p" } as KProperty<*>
    assertEquals(listOf(KParameter.Kind.CONTEXT), p.parameters.map { it.kind })
    assertEquals("pR", p.getter.call(ctx))

    return "OK"
}
