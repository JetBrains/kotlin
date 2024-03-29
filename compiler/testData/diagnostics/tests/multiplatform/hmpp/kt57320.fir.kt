// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common

// FILE: StringValue.kt
expect class StringValue

expect fun StringValue.plus(other: String): StringValue

// MODULE: commonJS()()(common)
// TARGET_PLATFORM: JS

// FILE: StringValueJs.kt
actual class StringValue(val value: String)

actual fun StringValue.plus(other: String) = StringValue(this.value + other)

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common

// FILE: StringDemoInterface.kt
expect interface StringDemoInterface

interface KotlinXStringDemoInterface {
    val value: String
}

expect fun StringDemoInterface.plusK(): String

// MODULE: js()()(common, intermediate)
// TARGET_PLATFORM: JS

// FILE: StringDemoInterfaceJs.kt
actual typealias StringDemoInterface = KotlinXStringDemoInterface

actual fun StringDemoInterface.<!ACTUAL_WITHOUT_EXPECT("actual fun StringDemoInterface.plusK(): <ERROR TYPE REF: Unresolved name: value>; The following declaration is incompatible because return type is different:    expect fun StringDemoInterface.plusK(): String")!>plusK<!>() = <!EXPECT_CLASS_AS_FUNCTION!>StringValue<!>(value).plus("K").<!UNRESOLVED_REFERENCE!>value<!>

// FILE: main.kt
class StringDemo(override val value: String) : StringDemoInterface

fun box() = StringDemo("O").plusK()
