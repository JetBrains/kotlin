// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses +NameBasedDestructuring -DeprecateNameMismatchInShortDestructuringWithParentheses -EnableNameBasedDestructuringShortForm
// ISSUE: KT-87696

interface HasTokenId {
    val id: Int
}

value class SmartToken(override val id: Int) : HasTokenId

fun readAfterSmartCast(value: HasTokenId) {
    if (value is SmartToken) {
        val (id) = <!COMPONENT_FUNCTION_MISSING!>value<!>
    }
}


/* GENERATED_FIR_TAGS: classDeclaration, destructuringDeclaration, equalityExpression, functionDeclaration, ifExpression,
integerLiteral, interfaceDeclaration, isExpression, localProperty, override, primaryConstructor, propertyDeclaration,
smartcast, value */
