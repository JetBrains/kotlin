// !LANGUAGE: +JvmRecordSupport

@JvmRecord
class BasicRecord(val x: String)

@JvmRecord
data class BasicDataRecord(val x: String)

@JvmRecord
class BasicRecordWithSuperClass(val x: String) : Record()

