// RUN_PIPELINE_TILL: CODEGEN
class My {
    companion object {
        fun My.foo() {}
    }

    fun test() {
        foo()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, funWithExtensionReceiver, functionDeclaration,
objectDeclaration */
