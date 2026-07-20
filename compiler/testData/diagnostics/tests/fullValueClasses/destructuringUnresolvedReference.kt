// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses +NameBasedDestructuring -DeprecateNameMismatchInShortDestructuringWithParentheses -EnableNameBasedDestructuringShortForm
// ISSUE: KT-87695

value class FullMoneyMismatch(val amount: Int, val currency: String)

 fun main() {
     val (<!UNRESOLVED_REFERENCE!>missing<!>) = FullMoneyMismatch(10, "EUR")
 }

/* GENERATED_FIR_TAGS: classDeclaration, destructuringDeclaration, functionDeclaration, integerLiteral, localProperty,
primaryConstructor, propertyDeclaration, stringLiteral, value */
