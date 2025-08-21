// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FIR_DUMP

class Base {
    private class Private

    fun test() {
        object {
            val x: Private = Private()

            init {
                val y: Private = Private()
            }

            fun foo() {
                val z: Private = Private()
            }
        }
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, init, localProperty,
nestedClass, propertyDeclaration */
