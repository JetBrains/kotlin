// !LANGUAGE: +LateinitTopLevelProperties
// MODULE: m1-common
// FILE: common.kt

<!NO_ACTUAL_FOR_EXPECT!>expect val justVal: String<!>
<!NO_ACTUAL_FOR_EXPECT!>expect var justVar: String<!>

<!NO_ACTUAL_FOR_EXPECT!>expect val String.extensionVal: Unit<!>
<!NO_ACTUAL_FOR_EXPECT!>expect var <T> T.genericExtensionVar: T<!>

<!NO_ACTUAL_FOR_EXPECT!>expect val valWithGet: String
    get<!>
<!NO_ACTUAL_FOR_EXPECT!>expect var varWithGetSet: String
    get set<!>

<!NO_ACTUAL_FOR_EXPECT!>expect var varWithPlatformGetSet: String
    <!WRONG_MODIFIER_TARGET!>expect<!> get
    <!WRONG_MODIFIER_TARGET!>expect<!> set<!>

<!NO_ACTUAL_FOR_EXPECT!>expect val backingFieldVal: String = <!EXPECTED_PROPERTY_INITIALIZER!>"no"<!><!>
<!NO_ACTUAL_FOR_EXPECT!>expect var backingFieldVar: String = <!EXPECTED_PROPERTY_INITIALIZER!>"no"<!><!>

<!NO_ACTUAL_FOR_EXPECT!>expect val customAccessorVal: String
    get() = "no"<!>
<!NO_ACTUAL_FOR_EXPECT!>expect var customAccessorVar: String
    get() = "no"
    set(value) {}<!>

<!NO_ACTUAL_FOR_EXPECT!>expect <!CONST_VAL_WITHOUT_INITIALIZER!>const<!> val constVal: Int<!>

<!NO_ACTUAL_FOR_EXPECT!>expect <!EXPECTED_LATEINIT_PROPERTY!>lateinit<!> var lateinitVar: String<!>

<!NO_ACTUAL_FOR_EXPECT!>expect val delegated: String by <!EXPECTED_DELEGATED_PROPERTY!>Delegate<!><!>
object Delegate { operator fun getValue(x: Any?, y: Any?): String = "" }

fun test(): String {
    <!WRONG_MODIFIER_TARGET!>expect<!> val localVariable: String
    localVariable = "no"
    return localVariable
}
