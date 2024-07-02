// LANGUAGE: +ValueClasses
// ALLOW_KOTLIN_PACKAGE

package kotlin.jvm

annotation class JvmInline

@JvmInline
value class FileSize(val bytesSize: Long, val x: Int): <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Comparable<!> {
    fun getBytes() = bytesSize
    fun getKB() = getBytes() / 1024
    fun getMB() = getKB() / 1024
    fun getGB() = getMB() / 1024
}
