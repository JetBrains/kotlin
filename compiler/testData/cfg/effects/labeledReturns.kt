// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE
// !LANGUAGE: +CalledInPlaceEffect

import kotlin.internal.*

inline fun <T, R> T.myLet(@CalledInPlace(InvocationCount.EXACTLY_ONCE) block: (T) -> R) = block(this)
inline fun myRun(@CalledInPlace(InvocationCount.EXACTLY_ONCE) block: () -> Unit) = block()
inline fun unknownRun(block: () -> Unit) = block()

fun getBool(): Boolean = false

fun threeLayersReturn(x: Int?): Int? {
    // Inner always jumps to outer
    // And middle always calls inner
    // So, in fact, middle never finished normally
    // Hence 'y = 54' in middle is unreachable, and middle doesn't performs definite initalization
    // Hence, outer doesn't performs definite initialization
    val y: Int
    myRun outer@ {
        myRun middle@ {
            x.myLet inner@ {
                if (it == null) {
                    return@outer Unit
                }
                else {
                    return@outer Unit
                }
            }
        }
        // Possible to report unreachable here
        y = 54
    }
    return y.inc()
}