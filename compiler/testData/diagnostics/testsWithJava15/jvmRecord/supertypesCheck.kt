// !LANGUAGE: +JvmRecordSupport

abstract class Abstract
interface I

@JvmRecord
class <!JVM_RECORD_EXTENDS_CLASS!>A1<!>(val x: String) : Abstract(), I

@JvmRecord
class <!JVM_RECORD_EXTENDS_CLASS!>A2<!>(val x: String) : Any(), I

@JvmRecord
class A3(val x: String) : Record(), I

@JvmRecord
class A4(val x: String) : java.lang.Record(), I

@JvmRecord
class A5(val x: String) : I
