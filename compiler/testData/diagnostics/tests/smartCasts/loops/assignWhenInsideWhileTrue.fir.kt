// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

public fun foo(x: String?): Int {
    var y: Any
    loop@ while (true) {
        y = when (x) {
            null -> break@loop
            "abc" -> return 0
            "xyz" -> return 1
            else -> x.length
        }         
        // y is always Int after when
        checkSubtype<Int>(y)
    }    
    // x is null because of the break
    return x<!UNSAFE_CALL!>.<!>length
}

/* GENERATED_FIR_TAGS: assignment, break, classDeclaration, equalityExpression, funWithExtensionReceiver,
functionDeclaration, functionalType, infix, integerLiteral, localProperty, nullableType, propertyDeclaration, smartcast,
stringLiteral, typeParameter, typeWithExtension, whenExpression, whenWithSubject, whileLoop */
