// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals
// RENDER_DIAGNOSTIC_ARGUMENTS
// WITH_STDLIB


<!INAPPLICABLE_OPERATOR_MODIFIER("must be a member of companion")!>operator<!> fun of(vararg ints: Int): IntArray = ints

<!INAPPLICABLE_OPERATOR_MODIFIER("must not have an extension receiver")!>operator<!> fun String.Companion.of(vararg chars: Char): String = chars.concatToString()

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, operator, vararg */
