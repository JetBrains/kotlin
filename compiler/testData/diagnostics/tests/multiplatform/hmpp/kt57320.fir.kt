// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// LANGUAGE: +MultiPlatformProjects

// MODULE: common

// FILE: StringValue.kt
<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> class StringValue

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> fun StringValue.plus(other: String): StringValue

// MODULE: commonJS()()(common)

// FILE: StringValueJs.kt
actual class StringValue(val value: String)

actual fun StringValue.plus(other: String) = StringValue(this.value + other)

// MODULE: intermediate()()(common)

// FILE: StringDemoInterface.kt
expect interface StringDemoInterface

interface KotlinXStringDemoInterface {
    val value: String
}

<!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM}!>expect<!> fun StringDemoInterface.plusK(): String

// MODULE: js()()(common, intermediate)

// FILE: StringDemoInterfaceJs.kt
actual typealias StringDemoInterface = KotlinXStringDemoInterface

actual fun StringDemoInterface.<!EXPECT_ACTUAL_INCOMPATIBLE_RETURN_TYPE!>plusK<!>() = <!EXPECT_CLASS_AS_FUNCTION!>StringValue<!>(value).plus("K").<!UNRESOLVED_REFERENCE!>value<!>

// FILE: main.kt
class StringDemo(override val value: String) : StringDemoInterface

fun box() = StringDemo("O").plusK()

/* GENERATED_FIR_TAGS: actual, additiveExpression, classDeclaration, expect, funWithExtensionReceiver,
functionDeclaration, interfaceDeclaration, override, primaryConstructor, propertyDeclaration, stringLiteral,
thisExpression, typeAliasDeclaration */
