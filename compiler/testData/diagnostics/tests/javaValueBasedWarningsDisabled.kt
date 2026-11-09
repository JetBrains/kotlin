// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// JDK version is important, because we rely on @ValueBased annotation being present on LocalDate class
// JDK_KIND: FULL_JDK_21

fun testSynchronized(p1: java.time.LocalDate) {
    synchronized(<!SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS!>p1<!>) { }
}

fun testIdentity(p1: java.time.LocalDate, p2: Any) {
    <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>p1<!> === p2
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, lambdaLiteral */
