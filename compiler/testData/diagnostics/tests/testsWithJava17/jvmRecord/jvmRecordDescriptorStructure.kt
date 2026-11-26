// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
// JVM_TARGET: 17

<!NON_DATA_CLASS_JVM_RECORD!>@JvmRecord<!>
class BasicRecord(val x: String)

@JvmRecord
data class BasicDataRecord(val x: String)

@JvmRecord
data class VarInConstructor(<!JVM_RECORD_NOT_VAL_PARAMETER!>var x: String<!>)

@JvmRecord
<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class BasicRecordWithSuperClass<!>(val x: String) : <!ILLEGAL_JAVA_LANG_RECORD_SUPERTYPE!>Record<!>()

/* GENERATED_FIR_TAGS: classDeclaration, data, primaryConstructor, propertyDeclaration */
