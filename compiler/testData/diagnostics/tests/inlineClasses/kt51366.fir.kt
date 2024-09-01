// LANGUAGE: +InlineClasses, -JvmInlineValueClasses

inline class FileSize(val bytesSize: Long): <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Comparable<!> {
    fun getBytes() = bytesSize
    fun getKB() = getBytes() / 1024
    fun getMB() = getKB() / 1024
    fun getGB() = getMB() / 1024
}
