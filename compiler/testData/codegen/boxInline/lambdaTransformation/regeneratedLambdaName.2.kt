package test

import kotlin.InlineOption.*

inline fun <R> call(crossinline f: () -> R) : R {
    return {f()} ()
}
