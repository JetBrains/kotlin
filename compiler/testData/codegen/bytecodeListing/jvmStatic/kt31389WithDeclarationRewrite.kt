// TARGET_BACKEND: JVM_IR
// JVM_TARGET: 1.8
// WITH_STDLIB
// REWRITE_JVM_STATIC_IN_COMPANION

annotation class Annotation {
    companion object {
        @JvmStatic val TEST_FIELD = "OK"

        var TEST_FIELD2 = ""
            @JvmStatic get
            @JvmStatic set
    }
}