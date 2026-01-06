// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

import java.io.IOException

class Foo {
    @JvmExposeBoxed
    @Throws(IOException::class)
    fun foo(i: UInt) {}
}

// LIGHT_ELEMENTS_NO_DECLARATION: Foo.class[foo-WZ4Q5Ns]