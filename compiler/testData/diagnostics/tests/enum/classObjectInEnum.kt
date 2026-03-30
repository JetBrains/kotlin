// RUN_PIPELINE_TILL: BACKEND
enum class E {
    ENTRY;

    companion object {
        fun entry() = ENTRY
    }
}

fun bar() = E.entry()

/* GENERATED_FIR_TAGS: companionObject, enumDeclaration, enumEntry, functionDeclaration, objectDeclaration */
