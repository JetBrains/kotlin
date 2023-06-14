// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR

// WITH_STDLIB

import kotlin.reflect.KClass

val <T : KClass<*>> T.myjava1: Class<*>
    get() = java

val <E : Any, T : KClass<E>> T.myjava2: Class<E>
    get() = java

class O
class K

fun box(): String =
        O::class.myjava1.getSimpleName() + K::class.myjava2.getSimpleName()

