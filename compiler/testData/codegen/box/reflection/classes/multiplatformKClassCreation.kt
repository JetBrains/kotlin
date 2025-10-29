// KT-77372
// WITH_REFLECT
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_PHASE: 2.2.0
// ^^^ KT-77372: Reflection intrinsics like `createKTypeParameter` have been moved to package `kotlin.reflect.js.internal` in 2.2.20-Beta1

import kotlin.test.assertEquals
import kotlin.reflect.KClass

fun box(): String {
    assertEquals("String", getKClass().simpleName)
    return "OK"
}

fun getKClass(): KClass<String> {
    val clazz = String::class
    return clazz
}