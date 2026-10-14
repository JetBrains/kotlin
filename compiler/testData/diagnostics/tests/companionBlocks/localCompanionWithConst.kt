// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocks

fun test() {
    class Local {
        companion {
            <!CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT!>const<!> val foo1 = "ABC"
        }
    }
}

class A {
    fun test() {
        class Local {
            companion {
                <!CONST_VAL_NOT_TOP_LEVEL_OR_OBJECT!>const<!> val foo2 = "ABC"
            }
        }
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, const, functionDeclaration, primaryConstructor,
propertyDeclaration, stringLiteral */
