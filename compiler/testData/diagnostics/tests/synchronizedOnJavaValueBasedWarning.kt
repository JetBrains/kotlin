// RUN_PIPELINE_TILL: CODEGEN
// TARGET_BACKEND: JVM
// WITH_STDLIB
// JDK version is important, because we rely on @ValueBased annotation being present on LocalDate class
// JDK_KIND: FULL_JDK_21

typealias LocalDateAlias = java.time.LocalDate

fun test(p1: java.time.LocalDate, p2: LocalDateAlias) {
    synchronized(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>p1<!>) { }
    synchronized(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>p2<!>) { }
    synchronized(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>Integer.valueOf(1)<!>) {}
}

fun testMostTypes() {
    synchronized(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>java.lang.Integer.valueOf(1)<!>) {}
    synchronized(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>java.lang.Byte.valueOf(1)<!>) {}
    synchronized(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>java.lang.Double.valueOf(1.0)<!>) {}
    synchronized(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>java.lang.Float.valueOf(1.0f)<!>) {}
    synchronized(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>java.lang.Long.valueOf(1)<!>) {}
    synchronized(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>java.lang.Short.valueOf(1)<!>) {}
    synchronized(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>java.lang.Character.valueOf('a')<!>) {}
    synchronized(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>java.lang.Boolean.valueOf(true)<!>) {}

    synchronized(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>java.lang.Runtime.version()<!>) {}

    synchronized(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>java.time.LocalDate.MIN<!>) {}

    synchronized(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>java.util.Optional.empty<Any>()<!>) {}
    synchronized(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>java.util.OptionalInt.empty()<!>) {}

    synchronized(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>java.time.LocalDate.MIN<!>) {}
    synchronized(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>java.time.chrono.JapaneseDate.now()<!>) {}

    synchronized(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>java.lang.ProcessHandle.current()<!>) {}
}

class TestField {
    val prop = java.lang.Integer.valueOf(1)
    fun method() = synchronized(<!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>prop<!>) {

    }
}

fun getVersion(): Runtime.Version {
    return Runtime.version()
}

fun testReturnVal() {
    synchronized(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>getVersion()<!>) {

    }
}

fun testLambda() {
    val version = getVersion()
    val lambda = {
        synchronized(<!IDENTITY_SENSITIVE_OPERATION_ON_WILL_BECOME_VALUE_CLASS!>version<!>) {

        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, integerLiteral, javaFunction, javaProperty,
lambdaLiteral, localProperty, propertyDeclaration, typeAliasDeclaration */
