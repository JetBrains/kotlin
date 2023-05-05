// FIR_IDENTICAL
// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// !API_VERSION: 1.5
// !LANGUAGE: +JvmRecordSupport
// SKIP_TXT
// JVM_TARGET: 17
// ENABLE_JVM_PREVIEW

interface I

val i: I = object : I {}

@JvmRecord
data class MyRec1(val name: String) : <!DELEGATION_BY_IN_JVM_RECORD!>I by i<!>

@JvmRecord
data class MyRec2(val name: String) {
    <!FIELD_IN_JVM_RECORD!>val x: Int = 0<!>
}

@JvmRecord
data class MyRec3(val name: String) {
    <!FIELD_IN_JVM_RECORD!>val y: String
        get() = field + "1"<!>

    init {
        y = ""
    }
}

@JvmRecord
data class MyRec4(val name: String) {
    <!FIELD_IN_JVM_RECORD!>val z: Int by lazy { 1 }<!>
}

@JvmRecord
data class MyRec5(val name: String) {
    val w: String get() = name + "1"
}






