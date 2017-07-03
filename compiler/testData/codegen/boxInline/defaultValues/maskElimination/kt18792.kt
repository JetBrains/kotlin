// FILE: 1.kt
//WITH_RUNTIME

package test

class SceneContainer2() {
    inline fun <reified T : CharSequence> pushTo(vararg injects: Any, time: A = 0.seconds, transition: B = b) = pushTo(T::class.java, *injects, time = time, transition = transition)

    fun <T : CharSequence> pushTo(clazz: Class<T>, vararg injects: Any, time: A = 0.seconds, transition: B = b): T {
        return "OK" as T
    }
}

class B

val b = B()

data class A(val x: Int)

inline val Number.seconds: A get() = A(toInt())

// FILE: 2.kt

import test.*

fun box(): String {
    return SceneContainer2().pushTo<String>(time = 0.2.seconds)
}
