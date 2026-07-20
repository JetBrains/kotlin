// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -CompanionBlocks -CompanionExtensions
// RENDER_DIAGNOSTIC_ARGUMENTS
<!UNSUPPORTED_FEATURE("The feature \"companion extensions\" is experimental and should be enabled explicitly. This can be done by supplying the compiler argument '-Xcompanion-blocks-and-extensions', but note that no stability guarantees are provided."), WRONG_MODIFIER_CONTAINING_DECLARATION("companion; file")!>companion<!> fun String.foo() {}
<!UNSUPPORTED_FEATURE("The feature \"companion extensions\" is experimental and should be enabled explicitly. This can be done by supplying the compiler argument '-Xcompanion-blocks-and-extensions', but note that no stability guarantees are provided."), WRONG_MODIFIER_CONTAINING_DECLARATION("companion; file")!>companion<!> val String.bar get() = 1

fun test() {
    String.<!UNRESOLVED_REFERENCE("foo")!>foo<!>()
    String.<!UNRESOLVED_REFERENCE("bar")!>bar<!>
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, getter, integerLiteral, propertyDeclaration,
propertyWithExtensionReceiver */
