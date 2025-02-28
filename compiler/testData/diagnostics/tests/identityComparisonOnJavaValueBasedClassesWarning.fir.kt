// RUN_PIPELINE_TILL: BACKEND
// TARGET_BACKEND: JVM
// WITH_STDLIB
// SKIP_TXT
// JDK version is important, because we rely on @ValueBased annotation being present on LocalDate class
// JDK_KIND: FULL_JDK_21

fun test(ld: java.time.LocalDate?, ld2: java.time.LocalDate) {
    <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>ld<!> === <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>ld2<!>
    <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>ld<!> !== <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>ld2<!>
    ld === null
    ld as Any === ld2 as Any
    <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>ld<!> === Any()
    Any() === <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>ld<!>
}
