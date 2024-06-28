// JVM_TARGET: 17
// COMPILER_ARGUMENTS: -XXLanguage:+JvmRecordSupport -Xjvm-enable-preview
package pkg

@JvmRecord
data class MyRec(val name: String)
