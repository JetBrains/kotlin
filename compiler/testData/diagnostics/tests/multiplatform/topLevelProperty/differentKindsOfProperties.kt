// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect val justVal: String
expect var justVar: String

expect val String.extensionVal: Unit
expect var <T> T.genericExtensionVar: T

expect val valWithGet: String
    get
expect var varWithGetSet: String
    get set

expect var varWithPlatformGetSet: String
    <!WRONG_MODIFIER_TARGET!>expect<!> get
    <!WRONG_MODIFIER_TARGET!>expect<!> set

expect val backingFieldVal: String = <!EXPECTED_PROPERTY_INITIALIZER!>"no"<!>
expect var backingFieldVar: String = <!EXPECTED_PROPERTY_INITIALIZER!>"no"<!>

expect val customAccessorVal: String
    <!EXPECTED_DECLARATION_WITH_BODY!>get()<!> = "no"
expect var customAccessorVar: String
    <!EXPECTED_DECLARATION_WITH_BODY!>get()<!> = "no"
    <!EXPECTED_DECLARATION_WITH_BODY!>set(value)<!> {}

expect <!CONST_VAL_WITHOUT_INITIALIZER!>const<!> val constVal: Int

<!INCOMPATIBLE_MODIFIERS!>expect<!> <!INCOMPATIBLE_MODIFIERS!>lateinit<!> var lateinitVar: String

<!WRONG_MODIFIER_TARGET!>expect<!> val delegated: String by Delegate
object Delegate { operator fun getValue(x: Any?, y: Any?): String = "" }

fun test(): String {
    <!WRONG_MODIFIER_TARGET!>expect<!> val localVariable: String
    localVariable = "no"
    return localVariable
}
