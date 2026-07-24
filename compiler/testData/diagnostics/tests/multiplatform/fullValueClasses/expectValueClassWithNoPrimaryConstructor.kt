// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +AllowExpectValueClassesWithNoPrimaryConstructor, +FullValueClasses
// IGNORE_FIR_DIAGNOSTICS
// WITH_STDLIB
// MODULE: common

expect value class CommonFinal {
    <!EXPECT_VALUE_CLASS_WITH_NO_PRIMARY_CONSTRUCTOR_HAS_SECONDARY("final value")!>constructor(value: Int)<!>
}

@kotlin.jvm.JvmInline
expect value class CommonJvmInline {
    <!EXPECT_VALUE_CLASS_WITH_NO_PRIMARY_CONSTRUCTOR_HAS_SECONDARY("@JvmInline value")!>constructor(value: Int)<!>
}

expect <!VALUE_CLASS_OPEN!>open<!> value class CommonOpen {
    constructor(value: Int)
}

expect abstract value class CommonAbstract {
    constructor(value: Int)
}

expect sealed value class CommonSealed {
    constructor(value: Int)
}

// MODULE: platform()()(common)

actual value class CommonFinal actual constructor(val value: Int)

@JvmInline
actual value class CommonJvmInline actual constructor(val value: Int)

actual <!VALUE_CLASS_OPEN!>open<!> value class CommonOpen actual constructor(val value: Int)

actual abstract value class CommonAbstract actual constructor(value: Int)

actual sealed value class CommonSealed actual constructor(value: Int)

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, primaryConstructor, propertyDeclaration, secondaryConstructor,
value */
