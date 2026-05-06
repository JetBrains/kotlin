// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions

class C {
    companion {
        <!COMPANION_BLOCK_NESTED!>companion<!> {}

        fun foo() {
            class Local {
                companion {
                    <!COMPANION_BLOCK_NESTED!>companion<!> {

                    }
                }
            }
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localClass */
