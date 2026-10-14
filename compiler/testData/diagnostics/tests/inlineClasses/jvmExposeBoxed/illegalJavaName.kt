// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89063
// WITH_STDLIB

@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class IC(val s: String)

@JvmExposeBoxed(<!JVM_EXPOSE_BOXED_NAME_IS_NOT_JAVA_IDENTIFIER!>"bad java name"<!>)
fun withSpaces(ic: IC) {}

@JvmExposeBoxed(<!JVM_EXPOSE_BOXED_NAME_IS_NOT_JAVA_IDENTIFIER!>"with-dash"<!>)
fun withDash(ic: IC) {}

@JvmExposeBoxed(<!JVM_EXPOSE_BOXED_NAME_IS_NOT_JAVA_IDENTIFIER!>"1digit"<!>)
fun leadingDigit(ic: IC) {}

@JvmExposeBoxed(<!JVM_EXPOSE_BOXED_NAME_IS_NOT_JAVA_IDENTIFIER!>"class"<!>)
fun javaKeyword(ic: IC) {}

@JvmExposeBoxed(<!JVM_EXPOSE_BOXED_NAME_IS_NOT_JAVA_IDENTIFIER!>"null"<!>)
fun javaReservedLiteral(ic: IC) {}

// Not even a valid JVM name: the existing error, and no warning on top of it
@JvmExposeBoxed(<!ILLEGAL_JVM_NAME!>"with/slash"<!>)
fun illegalJvmName(ic: IC) {}

// Contextual keywords are ordinary identifiers in Java
@JvmExposeBoxed("record")
fun contextualKeyword(ic: IC) {}

@JvmExposeBoxed("okName")
fun okDeclaration(ic: IC) {}

@JvmExposeBoxed("\uD801\uDC00")
fun surrogatePairIdentifier(ic: IC) {}

@JvmExposeBoxed("ok\uD801\uDC00")
fun surrogatePairIdentifierPart(ic: IC) {}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classDeclaration, classReference, functionDeclaration,
primaryConstructor, propertyDeclaration, stringLiteral, value */
