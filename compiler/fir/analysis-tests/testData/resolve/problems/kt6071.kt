// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-6071
// WITH_STDLIB

// KT-6071: USELESS_CAST warning message is misleading for non-supertype casts

fun test() {
    val x: String = "hello"
    // Cast to same type (not a super type) — the message should NOT say "You can use ':' if you need a cast to a super type."
    val y = x <!USELESS_CAST!>as String<!>

    // Cast to super type — the message could mention ':' syntax
    val z = x as Any
}

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration, localProperty, propertyDeclaration, stringLiteral */
