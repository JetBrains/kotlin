// RUN_PIPELINE_TILL: BACKEND
class Outer {
    fun foo() {
        class Local {
            fun bar() {
                val x = y
            }
        }
    }

    val y = ""
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localClass, localProperty, propertyDeclaration,
stringLiteral */
