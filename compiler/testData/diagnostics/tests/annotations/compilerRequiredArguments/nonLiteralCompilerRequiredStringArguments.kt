// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-87072
// WITH_STDLIB
// ALLOW_KOTLIN_PACKAGE

// FILE: jvmName.kt
package p

const val NAME = "name"

@JvmName(<!COMPILER_REQUIRED_ANNOTATION_ARGUMENT_MUST_BE_LITERAL_ERROR!>NAME<!>)
fun jvmNameConst() {}

@JvmName(<!COMPILER_REQUIRED_ANNOTATION_ARGUMENT_MUST_BE_LITERAL_ERROR!>"a" + "b"<!>)
fun jvmNameConcatenation() {}

@JvmName("literal")
fun jvmNameLiteral() {}

// FILE: deprecatedSince.kt
package kotlin.sub

const val SINCE = "1.0"

@Deprecated("")
@DeprecatedSinceKotlin(warningSince = <!COMPILER_REQUIRED_ANNOTATION_ARGUMENT_MUST_BE_LITERAL_ERROR!>SINCE<!>)
fun deprecatedSinceConst() {}

@Deprecated("")
@DeprecatedSinceKotlin(warningSince = "1.0")
fun deprecatedSinceLiteral() {}

/* GENERATED_FIR_TAGS: const, functionDeclaration, propertyDeclaration, stringLiteral */
