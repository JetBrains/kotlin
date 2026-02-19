// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP
class A(val a: String, val b: Int) {
    constructor(e: String): this(e, 0) {
        val message = "Secondary constructor body"
    }
    constructor(): this("") {
        val message = "Secondary constructor body with delegated constructor call"
    }
    init {
        val message = "Init body"
    }
}

class B private constructor(val a: String, val b: Int) {
    constructor(e: String): this(e, 0) {
        val message = "Secondary constructor body"
    }
    constructor(): this("") {
        val message = "Secondary constructor body with delegated constructor call"
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, primaryConstructor, propertyDeclaration,
secondaryConstructor */
