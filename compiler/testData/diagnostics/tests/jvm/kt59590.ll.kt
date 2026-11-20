// LL_FIR_DIVERGENCE
// LL FIR diagnostics tests don't run the JVM backend, so its diagnostics are not reported: `CONFLICTING_JVM_DECLARATIONS`.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT

object O {
    @JvmField
    val INSTANCE: O? = null
}

/* GENERATED_FIR_TAGS: nullableType, objectDeclaration, propertyDeclaration */
