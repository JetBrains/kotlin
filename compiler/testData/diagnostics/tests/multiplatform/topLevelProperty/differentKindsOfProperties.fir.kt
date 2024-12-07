// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// LANGUAGE: +LateinitTopLevelProperties
// MODULE: m1-common
// FILE: common.kt

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> val justVal: String
<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> var justVar: String

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> val String.extensionVal: Unit
<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> var <T> T.genericExtensionVar: T

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> val valWithGet: String
    get
<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> var varWithGetSet: String
    get set

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> var varWithPlatformGetSet: String
    <!WRONG_MODIFIER_TARGET!>expect<!> get
    <!WRONG_MODIFIER_TARGET!>expect<!> set

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> val backingFieldVal: String = <!EXPECTED_PROPERTY_INITIALIZER!>"no"<!>
<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> var backingFieldVar: String = <!EXPECTED_PROPERTY_INITIALIZER!>"no"<!>

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> val customAccessorVal: String
    <!EXPECTED_DECLARATION_WITH_BODY!>get()<!> = "no"
<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> var customAccessorVar: String
    <!EXPECTED_DECLARATION_WITH_BODY!>get()<!> = "no"
    <!EXPECTED_DECLARATION_WITH_BODY!>set(value)<!> {}

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> <!CONST_VAL_WITHOUT_INITIALIZER!>const<!> val constVal: Int

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> <!EXPECTED_LATEINIT_PROPERTY!>lateinit<!> var lateinitVar: String

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> val delegated: String by <!EXPECTED_DELEGATED_PROPERTY!>Delegate<!>
object Delegate { operator fun getValue(x: Any?, y: Any?): String = "" }

fun test(): String {
    <!WRONG_MODIFIER_TARGET!>expect<!> val localVariable: String
    localVariable = "no"
    return localVariable
}
