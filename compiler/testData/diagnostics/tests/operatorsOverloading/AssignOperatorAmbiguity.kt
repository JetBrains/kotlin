// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
//KT-1820 Write test for ASSIGN_OPERATOR_AMBIGUITY
package kt1820

class MyInt(val i: Int) {
    operator fun plus(m: MyInt) : MyInt = MyInt(m.i + i)
}

operator fun Any.plusAssign(a: Any) {}

fun test(m: MyInt) {
    m += m

    var i = 1
    i <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> 34
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration,
integerLiteral, localProperty, operator, primaryConstructor, propertyDeclaration */
