// ISSUE: KT-76990
// ALLOW_KOTLIN_PACKAGE
package kotlin.jvm

@JvmRecord
data class <caret>Some

annotation class Ann
typealias JvmRecord = Ann

