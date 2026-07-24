// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects +CompanionBlocks +CompanionExtensions
// WITH_STDLIB
// MODULE: common
// FILE: common.kt
interface I {
    companion {
        val x = 1
        const val y = 2
        <!INAPPLICABLE_JVM_FIELD!>@JvmField<!> val z = 3

        val getter get() = 1
    }
}

// MODULE: jvm()()(common)
// FILE: jvm.kt

/* GENERATED_FIR_TAGS: const, getter, integerLiteral, interfaceDeclaration, propertyDeclaration */
