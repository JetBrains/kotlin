// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// MODULE: common
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Typealiased<!>

annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Ann<!>(val p: kotlin.reflect.KClass<*>)

<!CONFLICTING_OVERLOADS!>@Ann(Typealiased::class)
expect fun test()<!>

<!CONFLICTING_OVERLOADS!>@Ann(<!CLASS_LITERAL_LHS_NOT_A_CLASS!>Array<Typealiased>::class<!>)
expect fun testInArray()<!>

// MODULE: main()()(common)
class TypealiasedImpl

actual typealias <!AMBIGUOUS_EXPECTS!>Typealiased<!> = TypealiasedImpl

@Ann(Typealiased::class)
actual fun <!AMBIGUOUS_EXPECTS!>test<!>() {}

@Ann(Array<Typealiased>::class)
actual fun <!AMBIGUOUS_EXPECTS!>testInArray<!>() {}
