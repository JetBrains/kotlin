// LL_FIR_DIVERGENCE
// Metadata diagnostic is missing
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects +CompanionBlocksAndExtensions
// WITH_STDLIB
// MODULE: common
// FILE: common.kt
interface I {
    companion {
        <!INTERFACE_COMPANION_BLOCK_PROPERTY_PRIVATE_FIELD!>val x<!> = 1
        const val y = 2
        @JvmField val z = 3

        val getter get() = 1
    }
}

// MODULE: jvm()()(common)
// FILE: jvm.kt

/* GENERATED_FIR_TAGS: const, getter, integerLiteral, interfaceDeclaration, propertyDeclaration */
