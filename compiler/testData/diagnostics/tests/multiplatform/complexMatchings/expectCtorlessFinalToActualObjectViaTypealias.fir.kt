// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

expect class E01
<!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM;JVM}!>expect<!> class E02<!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM}!>()<!>
<!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM}!>expect<!> open class E03

<!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM;JVM}!>expect<!> class E04 {
    <!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM}!>constructor()<!>
}

<!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM;JVM}!>expect<!> class E05<!EXPECT_ACTUAL_IR_MISMATCH{JVM}!>(e: E01)<!>
<!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM;JVM}!>expect<!> class E06 {
    <!EXPECT_ACTUAL_IR_MISMATCH{JVM}!>constructor(e: E02)<!>
}

<!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM}!>expect<!> interface I01

expect class M01 {
    fun foo()
}

<!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM;JVM;JVM}, NO_ACTUAL_FOR_EXPECT{JVM;JVM;JVM}!>expect<!> enum class ENUM01

<!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM;JVM}!>expect<!> annotation class ANNO01

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

object ActualObject {
    fun foo() {}
}

actual typealias E01 = ActualObject
actual typealias <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_SCOPE!>E02<!> = ActualObject
actual typealias <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_KIND!>E03<!> = ActualObject

actual typealias <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_SCOPE!>E04<!> = ActualObject

actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>E05<!> = ActualObject
actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>E06<!> = ActualObject

actual typealias <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_KIND!>I01<!> = ActualObject

actual typealias M01 = ActualObject

actual typealias <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_KIND, EXPECT_ACTUAL_INCOMPATIBLE_SUPERTYPES!>ENUM01<!> = ActualObject

actual typealias <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_KIND, EXPECT_ACTUAL_INCOMPATIBLE_SUPERTYPES!>ANNO01<!> = ActualObject

/* GENERATED_FIR_TAGS: actual, annotationDeclaration, classDeclaration, enumDeclaration, expect, functionDeclaration,
interfaceDeclaration, objectDeclaration, primaryConstructor, secondaryConstructor, typeAliasDeclaration */
