// Fails on native when run with `-Pkotlin.internal.native.test.cacheMode=STATIC_EVERYWHERE`.
// Looks like the call to `k` fails as `k` is not exported from the lib module.
// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE
// IGNORE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE
// MODULE: lib
// FILE: Z.kt
package z

private interface I {
     fun k() = "K"
}

interface G<T>

class Z : I, G<I> {
    val o = "O"
}

// MODULE: main(lib)
// FILE: box.kt
import z.Z

fun box() = Z().run { o + k() }