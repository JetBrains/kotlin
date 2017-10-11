package datagen.rtti.vtable1

import kotlin.test.*

abstract class Super {
    abstract fun bar()
}

class Foo : Super() {
    final override fun bar() {}
}

@Test fun runTest() {
    // This test now checks that the source can be successfully compiled and linked;
    // TODO: check the contents of TypeInfo?
}