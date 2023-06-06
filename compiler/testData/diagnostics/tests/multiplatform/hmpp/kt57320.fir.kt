// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common

// FILE: StringValue.kt
<!NO_ACTUAL_FOR_EXPECT{JS}!>expect class StringValue<!>

<!NO_ACTUAL_FOR_EXPECT{JS}!>expect fun StringValue.plus(other: String): StringValue<!>

// MODULE: commonJS()()(common)
// TARGET_PLATFORM: JS

// FILE: StringValue.kt
actual class Strin<!NO_ACTUAL_FOR_EXPECT{JS}!>gValue(val value: String<!>)
<!NO_ACTUAL_FOR_EXPECT{JS}!>
actual fun StringValue.plus(other: String) = StringVal<!>ue(this.value + other)

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common

// FILE: StringDemoInterface.kt
expect interface StringDemoInterface

interface KotlinXStringDemoInterface {
    val value: String
}

<!INCOMPATIBLE_MATCHING{JS}!>expect fun StringDemoInterface.plusK(): String<!>

// MODULE: js()()(common, intermediate)
// TARGET_PLATFORM: JS

// FILE: StringDemoInterface.kt
actual typealias StringDemoInterface = KotlinXStringDemoInterface

<!ACTUAL_WITHOUT_EXPECT("actual  fun StringDemoInterface.plusK(): <ERROR TYPE REF: Unresolved name: value>; The following declaration is incompatible:    expect fun StringDemoInterface.plusK(): String")!>actual fun StringDemoIn<!INCOMPATIBLE_MATCHING!>terface.plusK() = <!RESOLUTION_TO_CLASSIFIER!>StringValue<!>(value).plus("K")<!>.<!UNRESOLVED_REFERENCE!>value<!><!>

// FILE: main.kt
class StringDemo(override val value: String) : StringDemoInterface

fun box() = StringDemo("O").plusK()
