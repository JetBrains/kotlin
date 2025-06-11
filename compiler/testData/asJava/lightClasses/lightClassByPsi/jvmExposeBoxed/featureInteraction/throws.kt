// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

import java.io.IOException

class Foo {
    @JvmExposeBoxed
    @Throws(IOException::class)
    fun foo(i: UInt) {}
}
