// !LANGUAGE: +JvmRecordSupport
// SKIP_TXT

@JvmRecord
class <!JVM_RECORD_WITHOUT_PRIMARY_CONSTRUCTOR_PARAMETERS!>A0<!>

@JvmRecord
class <!JVM_RECORD_WITHOUT_PRIMARY_CONSTRUCTOR_PARAMETERS!>A1<!> {
    constructor()
}

@JvmRecord
class A2<!JVM_RECORD_WITHOUT_PRIMARY_CONSTRUCTOR_PARAMETERS!>()<!>

@JvmRecord
class A3(<!JVM_RECORD_NOT_VAL_PARAMETER!><!UNUSED_PARAMETER!>name<!>: String<!>)

@JvmRecord
class A4(<!JVM_RECORD_NOT_VAL_PARAMETER!>var name: String<!>)

@JvmRecord
class A5(vararg val name: String, <!JVM_RECORD_NOT_VAL_PARAMETER!><!UNUSED_PARAMETER!>y<!>: Int<!>)

@JvmRecord
class A6(
    val x: String,
    val y: Int,
    vararg val z: Double,
)

fun main() {
    <!LOCAL_JVM_RECORD!>@JvmRecord<!>
    class Local
}
