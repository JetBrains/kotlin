// !LANGUAGE: +JvmRecordSupport
// SKIP_TXT

interface I

val i: I = object : I {}

@JvmRecord
class MyRec1(val name: String) : <!DELEGATION_BY_IN_JVM_RECORD!>I by i<!>

@JvmRecord
class MyRec2(val name: String) {
    <!FIELD_IN_JVM_RECORD!>val x: Int = 0<!>
}

@JvmRecord
class MyRec3(val name: String) {
    <!FIELD_IN_JVM_RECORD!>val y: String
        get() = field + "1"<!>

    init {
        y = ""
    }
}

@JvmRecord
class MyRec4(val name: String) {
    <!FIELD_IN_JVM_RECORD!>val z: Int by lazy { 1 }<!>
}

@JvmRecord
class MyRec5(val name: String) {
    val w: String get() = name + "1"
}






