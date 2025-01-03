// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -TYPE_MISMATCH
// MODULE: common
expect annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Typealiased<!>()

annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Ann<!>(val p: Typealiased)

<!CONFLICTING_OVERLOADS!>@Ann(Typealiased())
expect fun test()<!>

// MODULE: main()()(common)
annotation class TypealiasedImpl

actual typealias <!AMBIGUOUS_EXPECTS!>Typealiased<!> = TypealiasedImpl

@Ann(Typealiased())
actual fun <!AMBIGUOUS_EXPECTS!>test<!>() {}

