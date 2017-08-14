// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

import kotlin.internal.*

inline fun <T, R> T.myLet(@CalledInPlace(InvocationCount.EXACTLY_ONCE) block: (T) -> R) = block(this)

fun nonLocalReturnWithElvis(x: Int?): Int? {
    x?.myLet { return 42 }
    return x?.inc()
}