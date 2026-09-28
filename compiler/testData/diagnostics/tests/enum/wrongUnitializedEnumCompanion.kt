// RUN_PIPELINE_TILL: CODEGEN
// See KT-20959

enum class Foo {;
    companion object  {
        val x = foo() // there should be no UNINITIALIZED_ENUM_COMPANION

        private fun foo() = "OK"
    }
}

/* GENERATED_FIR_TAGS: companionObject, enumDeclaration, functionDeclaration, objectDeclaration, propertyDeclaration,
stringLiteral */
