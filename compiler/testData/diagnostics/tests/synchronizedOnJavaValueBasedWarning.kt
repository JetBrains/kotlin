// RUN_PIPELINE_TILL: BACKEND
// TARGET_BACKEND: JVM
// WITH_STDLIB
// SKIP_TXT
// JDK version is important, because we rely on @ValueBased annotation being present on LocalDate class
// JDK_KIND: FULL_JDK_21

typealias LocalDateAlias = java.time.LocalDate

fun test(p1: java.time.LocalDate, p2: LocalDateAlias) {
    synchronized(p1) { }
    synchronized(p2) { }
    <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(Integer.valueOf(1)) {}
}

fun testMostTypes() {
    <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(java.lang.Integer.valueOf(1)) {}
    <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(java.lang.Byte.valueOf(1)) {}
    <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(java.lang.Double.valueOf(1.0)) {}
    <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(java.lang.Float.valueOf(1.0f)) {}
    <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(java.lang.Long.valueOf(1)) {}
    <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(java.lang.Short.valueOf(1)) {}
    <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(java.lang.Character.valueOf('a')) {}
    <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(java.lang.Boolean.valueOf(true)) {}

    synchronized(java.lang.Runtime.version()) {}

    synchronized(java.time.LocalDate.MIN) {}

    synchronized(java.util.Optional.empty<Any>()) {}
    synchronized(java.util.OptionalInt.empty()) {}

    synchronized(java.time.LocalDate.MIN) {}
    synchronized(java.time.chrono.JapaneseDate.now()) {}

    synchronized(java.lang.ProcessHandle.current()) {}
}

class TestField {
    val prop = java.lang.Integer.valueOf(1)
    fun method() = <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(prop) {

    }
}

fun getVersion(): Runtime.Version {
    return Runtime.version()
}

fun testReturnVal() {
    synchronized(getVersion()) {

    }
}

fun testLambda() {
    val version = getVersion()
    val lambda = {
        synchronized(version) {

        }
    }
}
