// FIR_IDENTICAL
enum class JvmTarget(val description: String) {
    JVM_1_6("1.6"),
    JVM_1_8("1.8"),
    JVM_9("9"),
    JVM_10("10"),
    JVM_11("11"),
    JVM_12("12"),
    JVM_13("13"),
    JVM_14("14"),
    JVM_15("15"),
    ;

    // Should not report UNINITIALIZED_ENUM_ENTRY
    val bytecodeVersion: String by lazy {
        when (this) {
            JVM_1_6 -> "Opcodes.V1_6"
            JVM_1_8 -> "Opcodes.V1_8"
            JVM_9 -> "Opcodes.V9"
            JVM_10 -> "Opcodes.V10"
            JVM_11 -> "Opcodes.V11"
            JVM_12 -> "Opcodes.V12"
            JVM_13 -> "Opcodes.V12 + 1"
            JVM_14 -> "Opcodes.V12 + 2"
            JVM_15 -> "Opcodes.V12 + 3"
        }
    }
}
