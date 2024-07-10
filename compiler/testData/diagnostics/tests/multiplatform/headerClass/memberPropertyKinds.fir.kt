// MODULE: m1-common
// FILE: common.kt

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect class Foo {
    val justVal: String
    var justVar: String

    val String.extensionVal: Unit
    var <T> T.genericExtensionVar: T

    val valWithGet: String
        get
    var varWithGetSet: String
        get set

    val backingFieldVal: String = <!EXPECTED_PROPERTY_INITIALIZER, EXPECTED_PROPERTY_INITIALIZER{METADATA}!>"no"<!>
    var backingFieldVar: String = <!EXPECTED_PROPERTY_INITIALIZER, EXPECTED_PROPERTY_INITIALIZER{METADATA}!>"no"<!>

    val customAccessorVal: String
    <!EXPECTED_DECLARATION_WITH_BODY, EXPECTED_DECLARATION_WITH_BODY{METADATA}!>get()<!> = "no"
    var customAccessorVar: String
    <!EXPECTED_DECLARATION_WITH_BODY, EXPECTED_DECLARATION_WITH_BODY{METADATA}!>get()<!> = "no"
    <!EXPECTED_DECLARATION_WITH_BODY, EXPECTED_DECLARATION_WITH_BODY{METADATA}!>set(value)<!> {}

    <!EXPECTED_LATEINIT_PROPERTY, EXPECTED_LATEINIT_PROPERTY{METADATA}!>lateinit<!> var lateinitVar: String

    val delegated: String by <!EXPECTED_DELEGATED_PROPERTY, EXPECTED_DELEGATED_PROPERTY{METADATA}!>Delegate<!>
}<!>

object Delegate { operator fun getValue(x: Any?, y: Any?): String = "" }
