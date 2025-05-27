// RUN_PIPELINE_TILL: BACKEND
// TARGET_BACKEND: JVM
// WITH_STDLIB
// SKIP_TXT
// JDK version is important, because we rely on @ValueBased annotation being present on LocalDate class
// JDK_KIND: FULL_JDK_21

typealias LocalDateAlias = java.time.LocalDate

fun test(p1: java.time.LocalDate, p2: LocalDateAlias) {
    synchronized(<!SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS!>p1<!>) { }
    synchronized(<!SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS!>p2<!>) { }
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

    synchronized(<!SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS!>java.lang.Runtime.version()<!>) {}

    synchronized(<!SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS!>java.time.LocalDate.MIN<!>) {}

    synchronized(<!SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS!>java.util.Optional.empty<Any>()<!>) {}
    synchronized(<!SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS!>java.util.OptionalInt.empty()<!>) {}

    synchronized(<!SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS!>java.time.LocalDate.MIN<!>) {}
    synchronized(<!SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS!>java.time.chrono.JapaneseDate.now()<!>) {}

    synchronized(<!SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS!>java.lang.ProcessHandle.current()<!>) {}
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
    synchronized(<!SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS!>getVersion()<!>) {

    }
}

fun testLambda() {
    val version = getVersion()
    val lambda = {
        synchronized(<!SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS!>version<!>) {

        }
    }
}
