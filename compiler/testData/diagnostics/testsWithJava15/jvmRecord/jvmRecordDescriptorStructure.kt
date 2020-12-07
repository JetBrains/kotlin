// !API_VERSION: 1.5
// !LANGUAGE: +JvmRecordSupport

<!NON_DATA_CLASS_JVM_RECORD!>@JvmRecord<!>
class BasicRecord(val x: String)

@JvmRecord
data class BasicDataRecord(val x: String)

@JvmRecord
class BasicRecordWithSuperClass(val x: String) : <!ILLEGAL_JAVA_LANG_RECORD_SUPERTYPE!>Record()<!>

