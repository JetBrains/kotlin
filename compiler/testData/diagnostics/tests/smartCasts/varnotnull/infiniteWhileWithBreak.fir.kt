// RUN_PIPELINE_TILL: FRONTEND
data class SomeObject(val n: SomeObject?) {
    fun doSomething(): Boolean = true
    fun next(): SomeObject? = n    
}


fun list(start: SomeObject) {
    var e: SomeObject?
    e = start
    // This comparison is senseless
    while (e != null) {
        // Smart cast because of the loop condition
        if (!e.doSomething())
            break
        // We still have smart cast here despite of a break
        e = e.next()
    } 
    // e can be null because of next()
    e<!UNSAFE_CALL!>.<!>doSomething()
}

/* GENERATED_FIR_TAGS: assignment, break, classDeclaration, data, equalityExpression, functionDeclaration, ifExpression,
localProperty, nullableType, primaryConstructor, propertyDeclaration, smartcast, whileLoop */
