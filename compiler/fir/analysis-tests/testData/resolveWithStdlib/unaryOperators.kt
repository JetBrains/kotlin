// RUN_PIPELINE_TILL: FRONTEND
class U {
    operator fun contains(g: String): Boolean {
        return false
    }
}


fun foo(u: U) {
    val b = false
    val i = 10
    val x = -i
    val y = !b
    val z = -1.0
    val w = +i

    val g = "" !in u
    val f = <!IMPOSSIBLE_IS_CHECK_ERROR!>"" !is Boolean<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, isExpression, localProperty, operator,
propertyDeclaration, stringLiteral, unaryExpression */
