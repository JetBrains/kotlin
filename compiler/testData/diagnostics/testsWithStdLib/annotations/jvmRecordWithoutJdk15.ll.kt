// LL_FIR_DIVERGENCE
// LL tests don't have jvmTargetProvider so they can't report JVM_RECORDS_ILLEGAL_BYTECODE_TARGET.
// See FirJvmRecordChecker
// ISSUE: KT-81100
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +JvmRecordSupport
// API_VERSION: 1.5
// SKIP_TXT

<!NON_DATA_CLASS_JVM_RECORD!>@JvmRecord<!>
class MyRec(
    val x: String,
    val y: Int,
    vararg val z: Double,
)

/* GENERATED_FIR_TAGS: classDeclaration, primaryConstructor, propertyDeclaration, vararg */
