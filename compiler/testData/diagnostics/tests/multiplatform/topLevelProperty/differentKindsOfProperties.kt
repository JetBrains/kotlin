// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

platform val justVal: String
platform var justVar: String

platform val String.extensionVal: Unit
platform var <T> T.genericExtensionVar: T

platform val valWithGet: String
    get
platform var varWithGetSet: String
    get set

platform var varWithPlatformGetSet: String
    <!WRONG_MODIFIER_TARGET!>platform<!> get
    <!WRONG_MODIFIER_TARGET!>platform<!> set

platform val backingFieldVal: String = <!PLATFORM_PROPERTY_INITIALIZER!>"no"<!>
platform var backingFieldVar: String = <!PLATFORM_PROPERTY_INITIALIZER!>"no"<!>

platform val customAccessorVal: String
    <!PLATFORM_DECLARATION_WITH_BODY!>get()<!> = "no"
platform var customAccessorVar: String
    <!PLATFORM_DECLARATION_WITH_BODY!>get()<!> = "no"
    <!PLATFORM_DECLARATION_WITH_BODY!>set(value)<!> {}

platform <!CONST_VAL_WITHOUT_INITIALIZER!>const<!> val constVal: Int

platform <!WRONG_MODIFIER_TARGET!>lateinit<!> var lateinitVar: String

<!WRONG_MODIFIER_TARGET!>platform<!> val delegated: String by Delegate
object Delegate { operator fun getValue(x: Any?, y: Any?): String = "" }

fun test(): String {
    <!WRONG_MODIFIER_TARGET!>platform<!> val localVariable: String
    localVariable = "no"
    return localVariable
}
