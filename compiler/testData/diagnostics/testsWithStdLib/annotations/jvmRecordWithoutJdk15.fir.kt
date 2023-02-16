// !LANGUAGE: +JvmRecordSupport
// !API_VERSION: 1.5
// SKIP_TXT

<!NON_DATA_CLASS_JVM_RECORD!>@JvmRecord<!>
class MyRec(
    val x: String,
    val y: Int,
    vararg val z: Double,
)
