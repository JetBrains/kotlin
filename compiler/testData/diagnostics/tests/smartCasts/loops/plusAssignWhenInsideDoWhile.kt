// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

fun bar(): Boolean { return true }

public fun foo(x: String?): Int {
    var y: Int?
    y = 0
    loop@ do {
        y += when (x) {
            null -> break@loop
            "abc" -> return 0
            "xyz" -> return 1
            else -> x.length
        }         
        // y is always Int after when
        checkSubtype<Int>(y)
    } while (bar())
    // y is always Int even here
    checkSubtype<Int>(y)
    // x is null because of the break
    return x<!UNSAFE_CALL!>.<!>length
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, break, classDeclaration, doWhileLoop, equalityExpression,
funWithExtensionReceiver, functionDeclaration, functionalType, infix, integerLiteral, localProperty, nullableType,
propertyDeclaration, smartcast, stringLiteral, typeParameter, typeWithExtension, whenExpression, whenWithSubject */
