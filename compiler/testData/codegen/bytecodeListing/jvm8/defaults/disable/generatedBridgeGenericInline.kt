// JVM_DEFAULT_MODE: disable
// WITH_STDLIB
// LANGUAGE: +ImplicitJvmExposeBoxed

interface Test<T> {
    fun test(p: T): T = null!!
}

class TestClass : Test<UInt>
