// JVM_TARGET: 1.8
// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-63984, KT-69075

annotation class Annotation {
    companion object {
        @JvmStatic val TEST_FIELD = "OK"

        var TEST_FIELD2 = ""
            @JvmStatic get
            @JvmStatic set
    }
}