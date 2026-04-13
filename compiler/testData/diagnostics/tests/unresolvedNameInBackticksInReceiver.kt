// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-60597

fun test() {
    <!UNRESOLVED_REFERENCE!>`java.lang.Short.TYPE`<!>.getConstructor(TODO())
}

/* GENERATED_FIR_TAGS: functionDeclaration */
