package datagen.rtti.abstract_super

import kotlin.test.*

abstract class Super

class Foo : Super()

@Test fun runTest() {
    // This test now checks that the source can be successfully compiled and linked;
    // TODO: check the contents of TypeInfo?
}
