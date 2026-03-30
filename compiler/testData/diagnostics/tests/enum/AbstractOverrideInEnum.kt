// RUN_PIPELINE_TILL: BACKEND
enum class E : T {
    ENTRY {
        override fun f() {
        }
    };

    abstract override fun f()
}

interface T {
    fun f()
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, interfaceDeclaration, override */
