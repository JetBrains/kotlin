// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-42096

enum class E {
    <!WRONG_MODIFIER_TARGET!>inline<!> E1 {
        override fun invoke() = 123
    };
    abstract fun invoke(): Int
}

fun main() {
    E.E1.invoke()
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration */
