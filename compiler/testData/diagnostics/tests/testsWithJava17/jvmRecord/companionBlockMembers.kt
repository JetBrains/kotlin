// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89457
// LANGUAGE: +CompanionBlocks
// JVM_TARGET: 17
// ENABLE_JVM_PREVIEW

@JvmRecord
data class MyRec(val name: String) {
    companion {
        val a0 = 0
        var a1: Int = 0
        const val a2 = 0
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, const, data, integerLiteral, primaryConstructor, propertyDeclaration */
