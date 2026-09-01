// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT

<!CONFLICTING_JVM_DECLARATIONS!>object O<!> {
    <!CONFLICTING_JVM_DECLARATIONS!>@JvmField
    val INSTANCE: O?<!> = null
}

/* GENERATED_FIR_TAGS: nullableType, objectDeclaration, propertyDeclaration */
