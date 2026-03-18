// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-35922

// KT-35922: Partial resolve test fails if argument expression type is not rewritten with SmartCastManager

fun foo(s: String): Int = s.length

fun test1(x: Any) {
    if (x is String) {
        val result = foo(x)
    }
}

fun test2(x: Any?) {
    x ?: return
    val result = x.hashCode()
}

fun test3(y: Any) {
    val len = when {
        y is String -> y.length
        else -> 0
    }
}

fun test4(a: Any, b: Any) {
    if (a is String && b is Int) {
        val s: String = a
        val i: Int = b
    }
}

fun test5(x: Any?) {
    if (x != null) {
        val h = foo(x as String)
    }
}

/* GENERATED_FIR_TAGS: andExpression, asExpression, elvisExpression, equalityExpression, functionDeclaration,
ifExpression, integerLiteral, isExpression, localProperty, nullableType, propertyDeclaration, smartcast, whenExpression */
