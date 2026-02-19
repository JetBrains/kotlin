// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-80469

class Test {
    val a: (Int) -> Number
        field: (Any) -> Int = { 1 }

    fun usage1() {
        val x: Int = a("")
    }

    fun usage2() {
        val x: Int = a.invoke("")
    }

    fun usage3(b: (Int) -> Number) {
        b <!UNCHECKED_CAST!>as (Any) -> Int<!>
        val y: Int = b("")
    }

    fun usage4(c: (Int) -> Number, b: (Any) -> Int) {
        if (c === b) {
            val x: Int = c("")
        }
    }
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, equalityExpression, explicitBackingField, functionDeclaration,
functionalType, ifExpression, integerLiteral, lambdaLiteral, localProperty, propertyDeclaration, smartcast,
stringLiteral */
