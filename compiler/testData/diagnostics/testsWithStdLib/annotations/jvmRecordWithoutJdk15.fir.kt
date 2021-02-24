// !LANGUAGE: +JvmRecordSupport
// !API_VERSION: 1.5
// SKIP_TXT

@JvmRecord
class MyRec(
    val x: String,
    val y: Int,
    vararg val z: Double,
)
