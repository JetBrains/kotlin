// !LANGUAGE: +JvmRecordSupport
// SKIP_TXT

@JvmRecord
class MyRec(
    val x: String,
    val y: Int,
    vararg val z: Double,
)
