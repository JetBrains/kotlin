// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// LANGUAGE: +LateinitTopLevelProperties
// MODULE: m1-common
// FILE: common.kt

expect val <!NO_ACTUAL_FOR_EXPECT!>justVal<!>: String
expect var <!NO_ACTUAL_FOR_EXPECT!>justVar<!>: String

expect val String.<!NO_ACTUAL_FOR_EXPECT!>extensionVal<!>: Unit
expect var <T> T.<!NO_ACTUAL_FOR_EXPECT!>genericExtensionVar<!>: T

expect val <!NO_ACTUAL_FOR_EXPECT!>valWithGet<!>: String
    get
expect var <!NO_ACTUAL_FOR_EXPECT!>varWithGetSet<!>: String
    get set

expect var <!NO_ACTUAL_FOR_EXPECT!>varWithPlatformGetSet<!>: String
    <!WRONG_MODIFIER_TARGET!>expect<!> get
    <!WRONG_MODIFIER_TARGET!>expect<!> set

expect val <!NO_ACTUAL_FOR_EXPECT!>backingFieldVal<!>: String = <!EXPECTED_PROPERTY_INITIALIZER!>"no"<!>
expect var <!NO_ACTUAL_FOR_EXPECT!>backingFieldVar<!>: String = <!EXPECTED_PROPERTY_INITIALIZER!>"no"<!>

expect val <!NO_ACTUAL_FOR_EXPECT!>customAccessorVal<!>: String
    <!EXPECTED_DECLARATION_WITH_BODY!>get()<!> = "no"
expect var <!NO_ACTUAL_FOR_EXPECT!>customAccessorVar<!>: String
    <!EXPECTED_DECLARATION_WITH_BODY!>get()<!> = "no"
    <!EXPECTED_DECLARATION_WITH_BODY!>set(value)<!> {}

expect <!CONST_VAL_WITHOUT_INITIALIZER!>const<!> val <!NO_ACTUAL_FOR_EXPECT!>constVal<!>: Int

expect <!EXPECTED_LATEINIT_PROPERTY!>lateinit<!> var <!NO_ACTUAL_FOR_EXPECT!>lateinitVar<!>: String

expect val <!NO_ACTUAL_FOR_EXPECT!>delegated<!>: String <!EXPECTED_DELEGATED_PROPERTY!>by Delegate<!>
object Delegate { operator fun getValue(x: Any?, y: Any?): String = "" }

fun test(): String {
    <!WRONG_MODIFIER_TARGET!>expect<!> val localVariable: String
    localVariable = "no"
    return localVariable
}
