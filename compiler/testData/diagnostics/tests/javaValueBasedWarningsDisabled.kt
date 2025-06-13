// RUN_PIPELINE_TILL: BACKEND
// TARGET_BACKEND: JVM
// WITH_STDLIB
// SKIP_TXT
// FIR_IDENTICAL
// LANGUAGE: +DisableWarningsForValueBasedJavaClasses
// JDK version is important, because we rely on @ValueBased annotation being present on LocalDate class
// JDK_KIND: FULL_JDK_21

fun testSynchronized(p1: java.time.LocalDate) {
    synchronized(p1) { }
}

fun testIdentity(p1: java.time.LocalDate, p2: Any) {
    p1 === p2
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, lambdaLiteral */
