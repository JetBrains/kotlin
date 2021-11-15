// JVM_TARGET: 1.8
// WITH_STDLIB

annotation class Annotation {
    companion object {
        @JvmStatic val TEST_FIELD = "OK"

        var TEST_FIELD2 = ""
            @JvmStatic get
            @JvmStatic set
    }
}