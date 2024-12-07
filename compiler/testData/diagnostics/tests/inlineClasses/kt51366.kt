// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +InlineClasses
// DIAGNOSTICS: -INLINE_CLASS_DEPRECATED

inline class FileSize(val bytesSize: Long): <!VALUE_CLASS_CANNOT_EXTEND_CLASSES, WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Comparable<!> {
    fun getBytes() = bytesSize
    fun getKB() = getBytes() / 1024
    fun getMB() = getKB() / 1024
    fun getGB() = getMB() / 1024
}
