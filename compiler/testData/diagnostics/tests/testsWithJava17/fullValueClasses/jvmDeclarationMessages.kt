// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// JVM_TARGET: 17
// RENDER_DIAGNOSTICS_FULL_TEXT

<!NON_DATA_VALUE_CLASS_JVM_RECORD!>@JvmRecord<!>
@JvmInline
value class InlineRecord(val x: Int)

value class Full(val x: Int, val y: Int) {
    <!SYNCHRONIZED_ON_VALUE_CLASS_ERROR!>@Synchronized<!>
    fun f() {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, primaryConstructor, propertyDeclaration, value */
