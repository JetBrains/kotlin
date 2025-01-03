// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// LANGUAGE: +LateinitTopLevelProperties
// MODULE: m1-common
// FILE: common.kt

expect val <!NO_ACTUAL_FOR_EXPECT{JVM}, REDECLARATION!>justVal<!>: String
expect var <!NO_ACTUAL_FOR_EXPECT{JVM}, REDECLARATION!>justVar<!>: String

expect val String.<!NO_ACTUAL_FOR_EXPECT{JVM}, REDECLARATION!>extensionVal<!>: Unit
expect var <T> T.<!NO_ACTUAL_FOR_EXPECT{JVM}, REDECLARATION!>genericExtensionVar<!>: T

expect val <!NO_ACTUAL_FOR_EXPECT{JVM}, REDECLARATION!>valWithGet<!>: String
    get
expect var <!NO_ACTUAL_FOR_EXPECT{JVM}, REDECLARATION!>varWithGetSet<!>: String
    get set

expect var <!NO_ACTUAL_FOR_EXPECT{JVM}, REDECLARATION!>varWithPlatformGetSet<!>: String
    <!WRONG_MODIFIER_TARGET, WRONG_MODIFIER_TARGET{JVM}!>expect<!> get
    <!WRONG_MODIFIER_TARGET, WRONG_MODIFIER_TARGET{JVM}!>expect<!> set

expect val <!NO_ACTUAL_FOR_EXPECT{JVM}, REDECLARATION!>backingFieldVal<!>: String = <!EXPECTED_PROPERTY_INITIALIZER, EXPECTED_PROPERTY_INITIALIZER{JVM}!>"no"<!>
expect var <!NO_ACTUAL_FOR_EXPECT{JVM}, REDECLARATION!>backingFieldVar<!>: String = <!EXPECTED_PROPERTY_INITIALIZER, EXPECTED_PROPERTY_INITIALIZER{JVM}!>"no"<!>

expect val <!NO_ACTUAL_FOR_EXPECT{JVM}, REDECLARATION!>customAccessorVal<!>: String
    <!EXPECTED_DECLARATION_WITH_BODY, EXPECTED_DECLARATION_WITH_BODY{JVM}!>get()<!> = "no"
expect var <!NO_ACTUAL_FOR_EXPECT{JVM}, REDECLARATION!>customAccessorVar<!>: String
    <!EXPECTED_DECLARATION_WITH_BODY, EXPECTED_DECLARATION_WITH_BODY{JVM}!>get()<!> = "no"
    <!EXPECTED_DECLARATION_WITH_BODY, EXPECTED_DECLARATION_WITH_BODY{JVM}!>set(value)<!> {}

expect <!CONST_VAL_WITHOUT_INITIALIZER, CONST_VAL_WITHOUT_INITIALIZER{JVM}!>const<!> val <!NO_ACTUAL_FOR_EXPECT{JVM}, REDECLARATION!>constVal<!>: Int

expect <!EXPECTED_LATEINIT_PROPERTY, EXPECTED_LATEINIT_PROPERTY{JVM}!>lateinit<!> var <!NO_ACTUAL_FOR_EXPECT{JVM}, REDECLARATION!>lateinitVar<!>: String

expect val <!NO_ACTUAL_FOR_EXPECT{JVM}, REDECLARATION!>delegated<!>: String <!EXPECTED_DELEGATED_PROPERTY, EXPECTED_DELEGATED_PROPERTY{JVM}!>by Delegate<!>
object <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Delegate<!> { operator fun getValue(x: Any?, y: Any?): String = "" }

<!CONFLICTING_OVERLOADS!>fun test(): String<!> {
    <!WRONG_MODIFIER_TARGET, WRONG_MODIFIER_TARGET{JVM}!>expect<!> val localVariable: String
    localVariable = "no"
    return localVariable
}

// MODULE: m1-jvm()()(m1-common)
