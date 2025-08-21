// RUN_PIPELINE_TILL: BACKEND
data class SomeObject(val n: SomeObject?) {
    fun doSomething() {}
    fun next(): SomeObject? = n    
}


fun list(start: SomeObject) {
    var e: SomeObject? = start
    while (e != null) {
        // While condition makes both smart casts possible
        <!DEBUG_INFO_SMARTCAST!>e<!>.doSomething()
        e = <!DEBUG_INFO_SMARTCAST!>e<!>.next()
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, data, equalityExpression, functionDeclaration, localProperty,
nullableType, primaryConstructor, propertyDeclaration, smartcast, whileLoop */
