// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects, +AllowExpectValueClassesWithNoPrimaryConstructor
// LANGUAGE_FEATURE_TOGGLED: StabilizeWillBecomeValueRestrictions
// WITH_STDLIB
// MODULE: common

@WillBecomeValue
expect class CommonFinal {
    <!EXPECT_WILL_BECOME_VALUE_CLASS_WITH_NO_PRIMARY_CONSTRUCTOR_HAS_SECONDARY_ERROR!>constructor(value: Int)<!>

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String
}

// MODULE: platform()()(common)

@WillBecomeValue
actual class CommonFinal actual constructor(val value: Int) {
    actual override fun equals(other: Any?): Boolean = other is CommonFinal && other.value == value
    actual override fun hashCode(): Int = value
    actual override fun toString(): String = "CommonFinal($value)"
}

/* GENERATED_FIR_TAGS: actual, andExpression, classDeclaration, equalityExpression, expect, functionDeclaration,
isExpression, nullableType, operator, override, primaryConstructor, propertyDeclaration, secondaryConstructor, smartcast,
stringLiteral */
