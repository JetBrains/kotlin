// LANGUAGE: +ContextParameters
// OPT_IN: kotlin.ExperimentalContextParameters
// TARGET_BACKEND: JVM_IR
// WITH_REFLECT

import kotlin.jvm.internal.Reflection
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

context(c: String)
fun withDefault(x: Int = 7): String = "$c-$x"

context(c: String, b: Boolean)
fun twoCtx(x: Int = 1, y: Int = 2): String = "$c-$b-$x-$y"

fun box(): String {
    val members = Reflection.getOrCreateKotlinPackage(object {}::class.java.enclosingClass).members

    val wd = members.single { it.name == "withDefault" } as KFunction<*>
    assertEquals(listOf(KParameter.Kind.CONTEXT, KParameter.Kind.VALUE), wd.parameters.map { it.kind })
    val ctx = wd.parameters[0]
    val x = wd.parameters[1]
    // context argument through the map, default used for the absent value parameter (mask bit 1, after the context bit)
    assertEquals("ctx-7", wd.callBy(mapOf(ctx to "ctx")))
    // everything provided, the $default path is not taken
    assertEquals("ctx-5", wd.callBy(mapOf(ctx to "ctx", x to 5)))
    // positional call for comparison
    assertEquals("ctx-5", wd.call("ctx", 5))
    // a context parameter is required and has no default: callBy without it must fail
    assertFailsWith<IllegalArgumentException> { wd.callBy(mapOf(x to 5)) }

    // two context parameters shift the value parameters' mask bits to positions 2 and 3
    val tc = members.single { it.name == "twoCtx" } as KFunction<*>
    val (c, b, tx, ty) = tc.parameters
    assertEquals(
        listOf(KParameter.Kind.CONTEXT, KParameter.Kind.CONTEXT, KParameter.Kind.VALUE, KParameter.Kind.VALUE),
        tc.parameters.map { it.kind },
    )
    assertEquals("ctx-true-1-42", tc.callBy(mapOf(c to "ctx", b to true, ty to 42)))
    assertEquals("ctx-false-8-2", tc.callBy(mapOf(c to "ctx", b to false, tx to 8)))
    assertEquals("ctx-true-1-2", tc.callBy(mapOf(c to "ctx", b to true)))

    return "OK"
}
