// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-87072
// WITH_STDLIB
// ALLOW_KOTLIN_PACKAGE

// FILE: jvmName.kt
package p

const val NAME = "name"

@JvmName(NAME)
fun jvmNameConst() {}

@JvmName("a" + "b")
fun jvmNameConcatenation() {}

@JvmName("literal")
fun jvmNameLiteral() {}

// FILE: deprecatedSince.kt
package kotlin.sub

const val SINCE = "1.0"

@Deprecated("")
@DeprecatedSinceKotlin(warningSince = SINCE)
fun deprecatedSinceConst() {}

@Deprecated("")
@DeprecatedSinceKotlin(warningSince = "1.0")
fun deprecatedSinceLiteral() {}

/* GENERATED_FIR_TAGS: const, functionDeclaration, propertyDeclaration, stringLiteral */
