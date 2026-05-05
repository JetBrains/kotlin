// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions
// LATEST_LV_DIFFERENCE

class C {
    fun foo() {}
    val prop = 1

    companion {
        fun foo() {}
        val prop = 1
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, propertyDeclaration */
