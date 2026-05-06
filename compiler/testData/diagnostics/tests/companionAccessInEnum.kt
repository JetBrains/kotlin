// RUN_PIPELINE_TILL: BACKEND
enum class A {
    X, Y;
    companion object {
        fun foo(): Int {
            return 1
        }
    }

    fun foo(): Int = Companion.foo()
}

/* GENERATED_FIR_TAGS: companionObject, enumDeclaration, enumEntry, functionDeclaration, integerLiteral,
objectDeclaration */
