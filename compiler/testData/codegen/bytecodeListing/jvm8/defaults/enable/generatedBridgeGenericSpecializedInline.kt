// JVM_DEFAULT_MODE: enable
// WITH_STDLIB
// LANGUAGE: +ImplicitJvmExposeBoxed

interface Root<T> {
    fun test(p: T): T = null!!
}

interface Specialized: Root<UInt>


class TestClass : Specialized
