// RUN_PIPELINE_TILL: BACKEND
class My(val x: Int?) {
    val y: Int? by lazy {
        var z = x
        while (z != null) {
            z = <!DEBUG_INFO_SMARTCAST!>z<!>.hashCode()
            if (<!DEBUG_INFO_SMARTCAST!>z<!> < 0) return@lazy z
            if (z == 0) z = null
        }
        return@lazy null
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, comparisonExpression, equalityExpression, ifExpression,
integerLiteral, lambdaLiteral, localProperty, nullableType, primaryConstructor, propertyDeclaration, propertyDelegate,
smartcast, whileLoop */
