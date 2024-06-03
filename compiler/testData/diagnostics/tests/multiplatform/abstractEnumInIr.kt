// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt
// ISSUE: KT-68734
expect enum class MMKVMode {
    SINGLE_PROCESS,
    MULTI_PROCESS,
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual enum class MMKVMode {
    SINGLE_PROCESS {
        override val rawValue: String = "single"
    },
    MULTI_PROCESS {
        override val rawValue: String = "multi"
    };

    abstract val rawValue: String
}
