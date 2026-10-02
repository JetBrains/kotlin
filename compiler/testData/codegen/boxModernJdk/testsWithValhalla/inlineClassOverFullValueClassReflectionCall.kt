// VALHALLA_VALUE_CLASSES
// LANGUAGE: +FullValueClasses
// WITH_REFLECT

import kotlin.reflect.full.primaryConstructor

value class Full(val a: Int, val b: Int)

@JvmInline
value class WrapFull(val f: Full)

fun withDefault(w: WrapFull = WrapFull(Full(1, 2))): Int = w.f.a

class WithDefault(val w: WrapFull = WrapFull(Full(3, 4)), val n: Int = 0)

fun box(): String {
    if (::withDefault.callBy(emptyMap()) != 1) return "Fail: withDefault"
    val constructor = WithDefault::class.primaryConstructor!!
    if (constructor.callBy(mapOf(constructor.parameters[1] to 5)).w != WrapFull(Full(3, 4))) return "Fail: WithDefault"
    return "OK"
}
