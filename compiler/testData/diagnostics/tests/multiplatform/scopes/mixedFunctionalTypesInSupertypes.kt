// LANGUAGE: +MultiPlatformProjects
// IGNORE_DIAGNOSTIC_API
// IGNORE_REVERSED_RESOLVE
//  Reason: MPP diagnostics are reported differentely in the compiler and AA

// MODULE: common
// FILE: common.kt
expect interface <!NO_ACTUAL_FOR_EXPECT!>I1<!><out R> {
    fun invoke(): R
}

expect interface <!NO_ACTUAL_FOR_EXPECT!>I2<!><out R> {
    suspend fun invoke(): R
}

<!CONFLICTING_INHERITED_MEMBERS, CONFLICTING_INHERITED_MEMBERS{JVM}!>expect interface <!NO_ACTUAL_FOR_EXPECT!>ExpectInterface<!><!> : <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES{JVM}!>I1<Int>, I2<Int><!>

<!CONFLICTING_INHERITED_MEMBERS, CONFLICTING_INHERITED_MEMBERS{JVM}!>interface CommonInterface<!> : <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES{JVM}!>I1<Int>, I2<Int><!>

// MODULE: jvm()()(common)
// FILE: main.kt

<!ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE!>actual typealias I1<R> = () -> R<!>
<!ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE!>actual typealias I2<R> = suspend () -> R<!>

<!CONFLICTING_INHERITED_MEMBERS!>actual interface ExpectInterface<!> : <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>I1<Int>, I2<Int><!>
