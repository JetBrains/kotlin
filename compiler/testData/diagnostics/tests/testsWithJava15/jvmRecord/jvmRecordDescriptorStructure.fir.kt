// !API_VERSION: 1.5
// !LANGUAGE: +JvmRecordSupport
// JVM_TARGET: 15
// ENABLE_JVM_PREVIEW

@JvmRecord
class BasicRecord(val x: String)

@JvmRecord
data class BasicDataRecord(val x: String)

@JvmRecord
class BasicRecordWithSuperClass(val x: String) : Record()

