// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +AllowExpectValueClassesWithNoPrimaryConstructor
// IGNORE_FIR_DIAGNOSTICS
// WITH_STDLIB
// MODULE: common

expect value class CommonSomething {
    <!EXPECT_VALUE_CLASS_WITH_NO_PRIMARY_CONSTRUCTOR_HAS_SECONDARY!>constructor(value: Int)<!>
}

// MODULE: platform()()(common)

@JvmInline
actual value class CommonSomething(val value: Int)

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, primaryConstructor, propertyDeclaration, secondaryConstructor,
value */
