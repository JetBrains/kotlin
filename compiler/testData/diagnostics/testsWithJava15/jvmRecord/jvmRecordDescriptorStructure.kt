// !API_VERSION: 1.5
// !LANGUAGE: +JvmRecordSupport

<!NON_DATA_CLASS_JVM_RECORD!>@JvmRecord<!>
class BasicRecord(val x: String)

@JvmRecord
data class BasicDataRecord(val x: String)

<!NON_DATA_CLASS_JVM_RECORD!>@JvmRecord<!>
class BasicRecordWithSuperClass(val x: String) : Record()

