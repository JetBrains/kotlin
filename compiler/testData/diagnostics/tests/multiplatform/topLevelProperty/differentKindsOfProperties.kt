// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// LANGUAGE: +LateinitTopLevelProperties
// MODULE: m1-common
// FILE: common.kt

expect val <!NO_ACTUAL_FOR_EXPECT{JVM}!>justVal<!>: String
expect var <!NO_ACTUAL_FOR_EXPECT{JVM}!>justVar<!>: String

expect val String.<!NO_ACTUAL_FOR_EXPECT{JVM}!>extensionVal<!>: Unit
expect var <T> T.<!NO_ACTUAL_FOR_EXPECT{JVM}!>genericExtensionVar<!>: T

expect val <!NO_ACTUAL_FOR_EXPECT{JVM}!>valWithGet<!>: String
    get
expect var <!NO_ACTUAL_FOR_EXPECT{JVM}!>varWithGetSet<!>: String
    get set

expect var <!NO_ACTUAL_FOR_EXPECT{JVM}!>varWithPlatformGetSet<!>: String
    <!WRONG_MODIFIER_TARGET, WRONG_MODIFIER_TARGET{JVM}!>expect<!> get
    <!WRONG_MODIFIER_TARGET, WRONG_MODIFIER_TARGET{JVM}!>expect<!> set

expect val <!NO_ACTUAL_FOR_EXPECT{JVM}!>backingFieldVal<!>: String = <!EXPECTED_PROPERTY_INITIALIZER, EXPECTED_PROPERTY_INITIALIZER{JVM}!>"no"<!>
expect var <!NO_ACTUAL_FOR_EXPECT{JVM}!>backingFieldVar<!>: String = <!EXPECTED_PROPERTY_INITIALIZER, EXPECTED_PROPERTY_INITIALIZER{JVM}!>"no"<!>

expect val <!NO_ACTUAL_FOR_EXPECT{JVM}!>customAccessorVal<!>: String
    <!EXPECTED_DECLARATION_WITH_BODY, EXPECTED_DECLARATION_WITH_BODY{JVM}!>get()<!> = "no"
expect var <!NO_ACTUAL_FOR_EXPECT{JVM}!>customAccessorVar<!>: String
    <!EXPECTED_DECLARATION_WITH_BODY, EXPECTED_DECLARATION_WITH_BODY{JVM}!>get()<!> = "no"
    <!EXPECTED_DECLARATION_WITH_BODY, EXPECTED_DECLARATION_WITH_BODY{JVM}!>set(value)<!> {}

expect <!CONST_VAL_WITHOUT_INITIALIZER, CONST_VAL_WITHOUT_INITIALIZER{JVM}!>const<!> val <!NO_ACTUAL_FOR_EXPECT{JVM}!>constVal<!>: Int

expect <!EXPECTED_LATEINIT_PROPERTY, EXPECTED_LATEINIT_PROPERTY{JVM}!>lateinit<!> var <!NO_ACTUAL_FOR_EXPECT{JVM}!>lateinitVar<!>: String

expect val <!NO_ACTUAL_FOR_EXPECT{JVM}!>delegated<!>: String <!EXPECTED_DELEGATED_PROPERTY, EXPECTED_DELEGATED_PROPERTY{JVM}!>by Delegate<!>
object Delegate { operator fun getValue(x: Any?, y: Any?): String = "" }

fun test(): String {
    <!WRONG_MODIFIER_TARGET, WRONG_MODIFIER_TARGET{JVM}!>expect<!> val localVariable: String
    localVariable = "no"
    return localVariable
}

// MODULE: m1-jvm()()(m1-common)
