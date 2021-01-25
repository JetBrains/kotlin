// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect class Foo {
    val justVal: String
    var justVar: String

    val String.extensionVal: Unit
    var <T> T.genericExtensionVar: T

    val valWithGet: String
        get
    var varWithGetSet: String
        get set

    val backingFieldVal: String = <!EXPECTED_PROPERTY_INITIALIZER!>"no"<!>
    var backingFieldVar: String = <!EXPECTED_PROPERTY_INITIALIZER!>"no"<!>

    val customAccessorVal: String
    get() = "no"
    var customAccessorVar: String
    get() = "no"
    set(value) {}

    lateinit var lateinitVar: String

    val delegated: String by <!EXPECTED_DELEGATED_PROPERTY!>Delegate<!>
}

object Delegate { operator fun getValue(x: Any?, y: Any?): String = "" }
