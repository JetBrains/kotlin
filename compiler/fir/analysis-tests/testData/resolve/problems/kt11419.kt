// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-11419

// KT-11419: Additional call checkers - tailrec: missing NON_TAIL_RECURSIVE_CALL in nested lambda

class Tree(val left: Tree? = null, val right: Tree? = null)

tailrec fun heightCPS(t: Tree?, k: (Int) -> Int): Int {
    if (t == null) return k(0)

    return heightCPS(t.left, fun(hl: Int): Int {
        return <!NON_TAIL_RECURSIVE_CALL!>heightCPS<!>(t.right, fun(hr: Int): Int {
            return k(if (hl > hr) hl else hr)
        })
    })
}

/* GENERATED_FIR_TAGS: anonymousFunction, classDeclaration, comparisonExpression, equalityExpression,
functionDeclaration, functionalType, ifExpression, integerLiteral, nullableType, primaryConstructor, propertyDeclaration,
smartcast, tailrec */
