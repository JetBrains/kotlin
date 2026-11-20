// LL_FIR_DIVERGENCE
// LL FIR diagnostics tests don't run the JVM backend, so its diagnostics are not reported: `CONFLICTING_JVM_DECLARATIONS`.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +CompanionBlocks
// WITH_STDLIB

@JvmInline
value class C1(val value: String) {
    companion {
        val value: String get() = "companion"
    }
}

@JvmInline
value class C2(val value: String) {
    companion {
        val value: String = "companion"
    }
}

@JvmInline
value class C3(val value: C1) {
    companion {
        val value: C1 get() = C1("companion")
    }
}

@JvmInline
value class C4(val value: C1) {
    companion {
        val value: C1 = C1("companion")
    }
}

@JvmInline
value class C5(val value: List<Int>) {
    companion {
        val value: List<String> = listOf()
    }
}

@JvmInline
value class C6(val value: Int) {
    companion {
        val value: String = "abc"
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, getter, primaryConstructor, propertyDeclaration, stringLiteral, value */
