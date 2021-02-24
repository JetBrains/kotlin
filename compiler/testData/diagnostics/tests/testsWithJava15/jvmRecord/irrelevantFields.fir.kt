// !API_VERSION: 1.5
// !LANGUAGE: +JvmRecordSupport
// SKIP_TXT
// JVM_TARGET: 15
// ENABLE_JVM_PREVIEW

interface I

val i: I = object : I {}

@JvmRecord
data class MyRec1(val name: String) : I by i

@JvmRecord
data class MyRec2(val name: String) {
    val x: Int = 0
}

@JvmRecord
data class MyRec3(val name: String) {
    val y: String
        get() = field + "1"

    init {
        y = ""
    }
}

@JvmRecord
data class MyRec4(val name: String) {
    val z: Int by lazy { 1 }
}

@JvmRecord
data class MyRec5(val name: String) {
    val w: String get() = name + "1"
}






