// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// !API_VERSION: 1.5
// !LANGUAGE: +JvmRecordSupport
// JVM_TARGET: 17
// ENABLE_JVM_PREVIEW

abstract class Abstract
interface I

@JvmRecord
data class <!JVM_RECORD_EXTENDS_CLASS!>A1<!>(val x: String) : Abstract(), I

@JvmRecord
data class <!JVM_RECORD_EXTENDS_CLASS!>A2<!>(val x: String) : Any(), I

@JvmRecord
data class A3(val x: String) : <!ILLEGAL_JAVA_LANG_RECORD_SUPERTYPE!>Record()<!>, I

@JvmRecord
data class A4(val x: String) : <!ILLEGAL_JAVA_LANG_RECORD_SUPERTYPE!>java.lang.Record()<!>, I

@JvmRecord
data class A5(val x: String) : I

data class A6(val x: String) : <!ILLEGAL_JAVA_LANG_RECORD_SUPERTYPE!>Record()<!>, I

data class A7(val x: String) : <!ILLEGAL_JAVA_LANG_RECORD_SUPERTYPE!>java.lang.Record()<!>, I

typealias TA = Record

data class A8(val x: String) : <!ILLEGAL_JAVA_LANG_RECORD_SUPERTYPE!>TA()<!>, I