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

fun getVersion(): Runtime.Version {
    return Runtime.version()
}

fun testReturnVal() {
    <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>getVersion()<!> === <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>getVersion()<!>
}

fun testLambda() {
    val version = getVersion()
    val lambda = {
        <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>version<!> === <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>getVersion()<!>
    }
}

fun testMultiple(x: <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Integer<!>) {
    <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>x<!> === <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>x<!> && <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>x<!> === <!IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE!>x<!>
}

/* GENERATED_FIR_TAGS: andExpression, asExpression, equalityExpression, flexibleType, functionDeclaration, javaFunction,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, smartcast */
