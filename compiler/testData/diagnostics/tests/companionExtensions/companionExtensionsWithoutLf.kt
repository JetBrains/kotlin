// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -CompanionBlocksAndExtensions
<!UNSUPPORTED_FEATURE, WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> fun String.foo() {}
<!UNSUPPORTED_FEATURE, WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> val String.bar get() = 1

fun test() {
    String.<!UNRESOLVED_REFERENCE!>foo<!>()
    String.<!UNRESOLVED_REFERENCE!>bar<!>
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, getter, integerLiteral, propertyDeclaration,
propertyWithExtensionReceiver */
