// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects
// MODULE: common
// FILE: common.kt
expect interface I1<out R> {
    fun invoke(): R
}

expect interface I2<out R> {
    suspend fun invoke(): R
}

expect interface ExpectInterface : I1<Int>, I2<Int>

<!CONFLICTING_INHERITED_MEMBERS!>interface CommonInterface<!> : I1<Int>, I2<Int>

// MODULE: jvm()()(common)
// FILE: main.kt

<!ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE!>actual typealias I1<R> = () -> R<!>
<!ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE!>actual typealias I2<R> = suspend () -> R<!>

<!CONFLICTING_INHERITED_MEMBERS!>actual interface <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>ExpectInterface<!><!> : <!MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES!>I1<Int>, I2<Int><!>
