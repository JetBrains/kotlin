// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

expect class <!NO_ACTUAL_FOR_EXPECT{JVM}!>Foo<!> {
    val justVal: String
    var justVar: String

    val String.extensionVal: Unit
    var <T> T.genericExtensionVar: T

    val valWithGet: String
        get
    var varWithGetSet: String
        get set

    val backingFieldVal: String = <!EXPECTED_PROPERTY_INITIALIZER, EXPECTED_PROPERTY_INITIALIZER{JVM}!>"no"<!>
    var backingFieldVar: String = <!EXPECTED_PROPERTY_INITIALIZER, EXPECTED_PROPERTY_INITIALIZER{JVM}!>"no"<!>

    val customAccessorVal: String
    <!EXPECTED_DECLARATION_WITH_BODY, EXPECTED_DECLARATION_WITH_BODY{JVM}!>get()<!> = "no"
    var customAccessorVar: String
    <!EXPECTED_DECLARATION_WITH_BODY, EXPECTED_DECLARATION_WITH_BODY{JVM}!>get()<!> = "no"
    <!EXPECTED_DECLARATION_WITH_BODY, EXPECTED_DECLARATION_WITH_BODY{JVM}!>set(value)<!> {}

    <!EXPECTED_LATEINIT_PROPERTY, EXPECTED_LATEINIT_PROPERTY{JVM}!>lateinit<!> var lateinitVar: String

    val delegated: String <!EXPECTED_DELEGATED_PROPERTY, EXPECTED_DELEGATED_PROPERTY{JVM}!>by Delegate<!>
}

object Delegate { operator fun getValue(x: Any?, y: Any?): String = "" }

// MODULE: m1-jvm()()(m1-common)
