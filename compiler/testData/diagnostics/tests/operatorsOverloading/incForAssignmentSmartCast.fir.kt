// RUN_PIPELINE_TILL: BACKEND
// SKIP_TXT
var c = 1

fun nullable(): Int? = null

fun foo(): Int {
    var x = nullable()
    if (x == null) {
        x = c++
    }

    return x
}

/* GENERATED_FIR_TAGS: assignment, equalityExpression, functionDeclaration, ifExpression, incrementDecrementExpression,
integerLiteral, localProperty, nullableType, propertyDeclaration, smartcast */
