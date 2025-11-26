// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-42096

enum class E {
    inline E1 {
        override fun invoke() = 123
    };
    abstract fun invoke(): Int
}

fun main() {
    E.E1.invoke()
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration */
