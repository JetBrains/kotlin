// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE_FEATURE_TOGGLED: ReportSubclassCantCallCompanionProtectedNonStaticWithGenerics
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
        <!SUBCLASS_CANT_CALL_COMPANION_PROTECTED_NON_STATIC_WARNING!>logger<!>.log("Hello")
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, nullableType, objectDeclaration,
propertyDeclaration, stringLiteral, typeParameter */
