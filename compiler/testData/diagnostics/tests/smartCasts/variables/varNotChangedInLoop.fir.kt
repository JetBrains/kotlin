// RUN_PIPELINE_TILL: BACKEND
public fun foo() {
    var i: Any = 1
    if (i is Int) {
        while (i != 10) {
            i++
        }
    }
}

/* GENERATED_FIR_TAGS: assignment, equalityExpression, functionDeclaration, ifExpression, incrementDecrementExpression,
integerLiteral, isExpression, localProperty, propertyDeclaration, smartcast, whileLoop */
