package test

import kotlin.InlineOption.*

inline fun <R> call(inlineOptions(ONLY_LOCAL_RETURN) f: () -> R) : R {
    return {f()} ()
}
