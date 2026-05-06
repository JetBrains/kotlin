// LL_FIR_DIVERGENCE
//   LL test doesn't report backend diagnostics
// LL_FIR_DIVERGENCE
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
