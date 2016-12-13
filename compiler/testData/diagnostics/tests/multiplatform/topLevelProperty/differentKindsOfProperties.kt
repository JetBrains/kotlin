// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

header val justVal: String
header var justVar: String

header val String.extensionVal: Unit
header var <T> T.genericExtensionVar: T

header val valWithGet: String
    get
header var varWithGetSet: String
    get set

header var varWithPlatformGetSet: String
    <!WRONG_MODIFIER_TARGET!>header<!> get
    <!WRONG_MODIFIER_TARGET!>header<!> set

header val backingFieldVal: String = <!HEADER_PROPERTY_INITIALIZER!>"no"<!>
header var backingFieldVar: String = <!HEADER_PROPERTY_INITIALIZER!>"no"<!>

header val customAccessorVal: String
    <!HEADER_DECLARATION_WITH_BODY!>get()<!> = "no"
header var customAccessorVar: String
    <!HEADER_DECLARATION_WITH_BODY!>get()<!> = "no"
    <!HEADER_DECLARATION_WITH_BODY!>set(value)<!> {}

header <!CONST_VAL_WITHOUT_INITIALIZER!>const<!> val constVal: Int

header <!WRONG_MODIFIER_TARGET!>lateinit<!> var lateinitVar: String

<!WRONG_MODIFIER_TARGET!>header<!> val delegated: String by Delegate
object Delegate { operator fun getValue(x: Any?, y: Any?): String = "" }

fun test(): String {
    <!WRONG_MODIFIER_TARGET!>header<!> val localVariable: String
    localVariable = "no"
    return localVariable
}
