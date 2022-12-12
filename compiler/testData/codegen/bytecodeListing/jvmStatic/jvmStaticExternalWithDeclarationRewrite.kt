// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// REWRITE_JVM_STATIC_IN_COMPANION
// JVM_TARGET: 1.8

object TestObject {
    @JvmStatic
    external fun foo()
}

class TestClassCompanion {
    companion object {
        @JvmStatic
        external fun foo()
    }
}
