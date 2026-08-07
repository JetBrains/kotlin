// RUN_PIPELINE_TILL: FRONTEND
// SKIP_JAVAC
// WITH_STDLIB

// '@JvmExposeBoxed' is marked '@ExperimentalStdlibApi', so every use site must opt in. There is deliberately
// no '@file:OptIn(ExperimentalStdlibApi::class)' in this file.

@JvmInline
value class IC(val s: String)

@<!OPT_IN_USAGE_ERROR!>JvmExposeBoxed<!>
fun topLevel(ic: IC): IC = ic

@<!OPT_IN_USAGE_ERROR!>JvmExposeBoxed<!>
class ClassLevel(val ic: IC)

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, primaryConstructor, propertyDeclaration, value */
