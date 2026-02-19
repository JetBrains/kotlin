// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// JAVAC_EXPECTED_FILE
// KT-588 Unresolved static method

class Test() : Thread("Test") {
    companion object {
        fun init2() {

        }
    }
    override fun run() {
        init2()      // unresolved
        Test.init2() // ok
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, objectDeclaration, override,
primaryConstructor, stringLiteral */
