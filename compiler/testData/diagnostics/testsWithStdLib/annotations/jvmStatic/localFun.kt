// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE
fun main() {
    <!JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION!>@JvmStatic fun a()<!>{

    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, localFunction */
