// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
// API_VERSION: 1.5
// LANGUAGE: +JvmRecordSupport
// SKIP_TXT
// JVM_TARGET: 17
// ENABLE_JVM_PREVIEW

<!NON_DATA_CLASS_JVM_RECORD!>@JvmRecord<!>
class A0

<!NON_DATA_CLASS_JVM_RECORD!>@JvmRecord<!>
class A1 {
    constructor()
}

<!NON_DATA_CLASS_JVM_RECORD!>@JvmRecord<!>
class A2()

<!NON_DATA_CLASS_JVM_RECORD!>@JvmRecord<!>
class A3(name: String)

<!NON_DATA_CLASS_JVM_RECORD!>@JvmRecord<!>
class A4(var name: String)

<!NON_DATA_CLASS_JVM_RECORD!>@JvmRecord<!>
class A5(vararg val name: String, y: Int)

@JvmRecord
<!NON_FINAL_JVM_RECORD!>open<!> class A6(val x: String)

@JvmRecord
<!NON_FINAL_JVM_RECORD!>abstract<!> class A7(val x: String)

@JvmRecord
<!NON_FINAL_JVM_RECORD!>sealed<!> class A8(val x: String)

@JvmRecord
<!ENUM_JVM_RECORD!>enum<!> class A9(val x: String) {
    X("");
}

<!NON_DATA_CLASS_JVM_RECORD!>@JvmRecord<!>
class A10(
    val x: String,
    val y: Int,
    vararg val z: Double,
)

fun main() {
    <!LOCAL_JVM_RECORD!>@JvmRecord<!>
    class Local
}

class Outer {
    @JvmRecord
    <!INNER_JVM_RECORD!>inner<!> class Inner(val name: String)
}

@JvmRecord
data class A11(<!DATA_CLASS_VARARG_PARAMETER, JVM_RECORD_NOT_LAST_VARARG_PARAMETER!>vararg val x: String<!>, val y: Int)
