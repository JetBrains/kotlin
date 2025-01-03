// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: common

<!CONFLICTING_OVERLOADS!>expect fun parameterCount()<!>
<!CONFLICTING_OVERLOADS!>fun parameterCount(p: String)<!> {}

<!CONFLICTING_OVERLOADS!>expect fun parameterCount2()<!>
<!CONFLICTING_OVERLOADS!>actual fun parameterCount2(p: String)<!> {}

<!CONFLICTING_OVERLOADS!>expect fun callableKind(): Int<!>
val <!REDECLARATION!>callableKind<!>: Int = 1

<!CONFLICTING_OVERLOADS!>expect fun <T> typeParameterCount()<!>
<!CONFLICTING_OVERLOADS!>fun typeParameterCount()<!> {}

expect <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>enum class <!PACKAGE_OR_CLASSIFIER_REDECLARATION, PACKAGE_OR_CLASSIFIER_REDECLARATION!>EnumEntries<!><!> {
    ONE, TWO;
}
actual <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>enum class <!PACKAGE_OR_CLASSIFIER_REDECLARATION, PACKAGE_OR_CLASSIFIER_REDECLARATION!>EnumEntries<!><!> {
    ONE;
}

<!CONFLICTING_OVERLOADS!>expect fun vararg(bar: Int)<!>
<!CONFLICTING_OVERLOADS!>fun vararg(vararg bar: Int)<!> = Unit

// MODULE: main()()(common)

actual fun <!AMBIGUOUS_EXPECTS!>parameterCount<!>() {}
actual fun <T> <!AMBIGUOUS_EXPECTS!>typeParameterCount<!>() {}
actual fun <!AMBIGUOUS_EXPECTS!>callableKind<!>(): Int = 1
actual fun <!AMBIGUOUS_EXPECTS!>vararg<!>(bar: Int) = Unit
