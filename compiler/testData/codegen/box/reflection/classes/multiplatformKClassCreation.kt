// KT-77372
// WITH_REFLECT
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: 2.2.0
// ^^^ KT-77372: Reflection intrinsics like `createKTypeParameter` have been moved to package `kotlin.reflect.js.internal` in 2.2.20-Beta1
//     This regression test is written to make sure there's no clash between `getKClass` symbols:
//     - user-defined one (as in this test)
//     - stdlib's one, starting from stdlib 2.2.20
//     Linking this test to stdlib 2.2.0, the clash will happen, reproducing user's issue

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