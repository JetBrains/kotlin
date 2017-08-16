// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE
// !LANGUAGE: +CalledInPlaceEffect

import kotlin.internal.*

inline fun <T> myRun(@CalledInPlace(InvocationCount.EXACTLY_ONCE) block: () -> T): T = block()

fun getBoolean(): Boolean = false

fun getBoolean() = false

fun test() {
    val x: Int

    if (getBoolean())
        run {
            while (getBoolean()) {
                do {
                    run {
                        if (getBoolean()) {
                            x = 42
                        } else {
                            x = 43
                        }
                    }
                    break
                } while (getBoolean())
                run { x.inc() }
                run { x = 42 }
                break
            }
            x = 42
        }
    else
        run {
            x = 42
        }

    x.inc()
}