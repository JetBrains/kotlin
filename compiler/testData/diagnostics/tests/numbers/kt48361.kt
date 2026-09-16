// RUN_PIPELINE_TILL: CODEGEN

fun foo(ttlMillis: Long = 5 * 60 * 1000) {}

const val cacheSize: Long = 4096 * 4

/* GENERATED_FIR_TAGS: const, functionDeclaration, integerLiteral, propertyDeclaration */
