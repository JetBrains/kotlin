// LANGUAGE: +MultiPlatformProjects
// IGNORE_DIAGNOSTIC_API
// IGNORE_REVERSED_RESOLVE
//  Reason: MPP diagnostics are reported differentely in the compiler and AA

// MODULE: common
// FILE: common.kt
expect interface I1<out R> {
    fun invoke(): R
}

expect interface I2<out R> {
    suspend fun invoke(): R
}

<!AMBIGUOUS_ACTUALS{JVM}, AMBIGUOUS_ACTUALS{JVM}, EXPECT_ACTUAL_INCOMPATIBILITY{JVM}, NO_ACTUAL_FOR_EXPECT{JVM}, NO_ACTUAL_FOR_EXPECT{JVM}!>expect interface ExpectInterface : I1<Int>, I2<Int><!>

interface CommonInterface : <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>I1<Int>, I2<Int><!>

// MODULE: jvm()()(common)
// FILE: main.kt

<!ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE!>actual typealias I1<R> = () -> R<!>
<!ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE!>actual typealias I2<R> = suspend () -> R<!>

actual interface <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>ExpectInterface<!> : <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>I1<Int>, I2<Int><!>
