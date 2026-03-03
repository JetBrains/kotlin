// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -CompanionBlocksAndExtensions

<!WRONG_MODIFIER_TARGET!>companion<!> fun String.foo() {}
<!WRONG_MODIFIER_TARGET!>companion<!> val String.bar get() = 1

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, getter, integerLiteral, propertyDeclaration,
propertyWithExtensionReceiver */
