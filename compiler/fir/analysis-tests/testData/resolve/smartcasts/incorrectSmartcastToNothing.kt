// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// FULL_JDK
// DUMP_CFG

import java.io.File

val cache: File? = File("foo")

fun test(cacheExtSetting: String?) {
    val cacheBaseDir = when {
        cacheExtSetting == null -> cache?.let { File(it, "main.kts.compiled.cache") }
        cacheExtSetting.isBlank() -> null
        else -> File(cacheExtSetting)
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, javaFunction, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, safeCall, smartcast, stringLiteral, whenExpression */
