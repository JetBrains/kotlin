// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common

// FILE: StringValue.kt
expect class <!NO_ACTUAL_FOR_EXPECT{JS}!>StringValue<!>

expect fun StringValue.<!NO_ACTUAL_FOR_EXPECT{JS}!>plus<!>(other: String): StringValue

// MODULE: commonJS()()(common)
// TARGET_PLATFORM: JS

// FILE: StringValue.kt
actual class StringValue(val value: String)

actual fun StringValue.plus(other: String) = StringValue(this.value + other)

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common

// FILE: StringDemoInterface.kt
expect interface StringDemoInterface

interface KotlinXStringDemoInterface {
    val value: String
}

expect fun StringDemoInterface.plusK(): <!NO_ACTUAL_FOR_EXPECT{JS}!>String<!>

// MODULE: js()()(common, intermediate)
// TARGET_PLATFORM: JS

// FILE: StringDemoInterface.kt
actual typealias <!ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER!>StringDemoInterface<!> = KotlinXStringDemoInterface

actual fun StringDemoInterface.<!ACTUAL_WITHOUT_EXPECT("Actual function 'plusK'; The following declaration is incompatible because return type is different:    public expect fun StringDemoInterface /* = KotlinXStringDemoInterface */.plusK(): String")!>plusK<!>() = <!RESOLUTION_TO_CLASSIFIER!>StringValue<!>(value).<!DEBUG_INFO_MISSING_UNRESOLVED!>plus<!>("K").<!DEBUG_INFO_MISSING_UNRESOLVED!>value<!>

// FILE: main.kt
class StringDemo(override val value: String) : StringDemoInterface

fun box() = StringDemo("O").plusK()
