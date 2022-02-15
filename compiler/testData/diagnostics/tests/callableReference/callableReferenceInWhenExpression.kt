// FIR_IDENTICAL
// ISSUE: KT-51272

import kotlin.reflect.KClass

fun testOnKClass(rootClass: KClass<Any>): Int {
    return when (rootClass) {
        Collection::class -> 1
        else -> 2
    }
}

fun testOnClass(rootClass: Class<Any>): Int {
    return when (rootClass) {
        Collection::class -> 1
        else -> 2
    }
}