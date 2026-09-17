// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89457
// LANGUAGE: +CompanionBlocks
// JVM_TARGET: 17
// ENABLE_JVM_PREVIEW

@JvmRecord
data class MyRec(val name: String) {
    companion {
        <!FIELD_IN_JVM_RECORD!>val a0<!> = 0
        <!FIELD_IN_JVM_RECORD!>var a1: Int<!> = 0
        const <!FIELD_IN_JVM_RECORD!>val a2<!> = 0
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, const, data, integerLiteral, primaryConstructor, propertyDeclaration */
