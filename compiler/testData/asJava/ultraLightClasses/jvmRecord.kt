// CHECK_BY_JAVA_FILE
// API_VERSION: 1.5
// JVM_TARGET: 17
// COMPILER_ARGUMENTS: -XXLanguage:+JvmRecordSupport -Xjvm-enable-preview
// IGNORE_FIR
//   reason: test framework does not support using JDK 17 as dependency
package pkg

@JvmRecord
data class MyRec(val name: String)
// JDK 15 should be used to compile this file
// COMPILATION_ERRORS
