// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// LANGUAGE: +ValueClasses

import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions

@JvmInline
value class S2(val x: Int, val y: Int)
@JvmInline
value class S4(val x: S2, val y: S2)
@JvmInline
value class S8(val x: S4, val y: S4)
@JvmInline
value class S16(val x: S8, val y: S8)
@JvmInline
value class S32(val x: S16, val y: S16)
@JvmInline
value class S64(val x: S32, val y: S32)

data class D(val s: S64 = createS64())

fun createS64(): S64 {
    val s2 = S2(0, 0)
    val s4 = S4(s2, s2)
    val s8 = S8(s4, s4)
    val s16 = S16(s8, s8)
    val s32 = S32(s16, s16)
    return S64(s32, s32)
}

fun box(): String {
    val copyFun = D::class.memberFunctions.single { it.name == "copy" }
    copyFun.callBy(mapOf(copyFun.instanceParameter!! to D()))
    return "OK"
}
