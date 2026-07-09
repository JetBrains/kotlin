// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-85417

class LoggerDummy {
    fun log(message: String) {}
}

abstract class Parent<T> {
    companion object {
        protected val logger = LoggerDummy()
    }
}

class Child : Parent<String>() {
    fun doStuff() {
        logger.log("Hello")
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, nullableType, objectDeclaration,
propertyDeclaration, stringLiteral, typeParameter */
